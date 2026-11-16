"""Repeatable real HTTP checks. Never prints credentials, keys or JWT values."""
from pathlib import Path
import importlib.util, json, os, subprocess, sys, time, urllib.request, urllib.error

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('auth_env',ROOT/'scripts/auth-env.py')
env=importlib.util.module_from_spec(spec);spec.loader.exec_module(env)
cfg=env.config()
BASE=os.environ.get('GATEWAY_URL','http://127.0.0.1:'+cfg.get('GATEWAY_PORT','8080'))
def request(method,path,body=None,token=None,headers=None):
    h={'Content-Type':'application/json',**(headers or {})}
    if token:h['Authorization']='Bearer '+token
    req=urllib.request.Request(BASE+path,data=json.dumps(body).encode() if body is not None else None,headers=h,method=method)
    try:
        response=urllib.request.urlopen(req,timeout=20)
    except urllib.error.HTTPError as e:response=e
    with response:
        raw=response.read().decode();data=json.loads(raw) if raw else None
        return response.status,data,response.headers
def check(response,status,label,route=None):
    actual,_,headers=response
    assert actual==status,f'{label}: expected {status}, got {actual}'
    if route:assert headers.get('X-Gateway-Route')==route,f'{label}: wrong route'
    print(f'PASS {label}: HTTP {actual}'+(f' -> {route}' if route else ''),flush=True)
    return response[1]
def login(name,key):
    data=check(request('POST','/api/auth/login',{'username':name,'password':cfg[key]}),200,'login '+name,'user-service')
    assert data['token_type']=='Bearer' and data['expires_in']==1800
    print('  access_token=<redacted>; roles='+','.join(data['roles']),flush=True)
    return data['access_token']
def main():
    print('CampusTrade Week 09 API Gateway / JWT | '+time.strftime('%Y-%m-%d %H:%M:%S'),flush=True)
    for i in range(90):
        try:
            if request('GET','/actuator/health')[0]==200:break
        except (OSError,TimeoutError):pass
        time.sleep(2)
    else:raise AssertionError('Gateway not ready')
    check(request('GET','/actuator/health'),200,'public health')
    check(request('GET','/api/items'),401,'1. missing JWT')
    check(request('GET','/api/admin/users',headers={'X-User-Id':'admin','X-User-Roles':'ADMIN'}),401,'forged headers without JWT')
    check(request('POST','/api/auth/login',{'username':'student','password':'incorrect'}),401,'wrong password')
    user=login('student','AUTH_USER_PASSWORD');admin=login('admin','AUTH_ADMIN_PASSWORD')
    me=check(request('GET','/api/auth/me',token=user),200,'2. authorized user','user-service')
    admin_me=check(request('GET','/api/auth/me',token=admin),200,'admin identity','user-service')
    check(request('GET','/api/items',token=user),200,'3. product route','product-service')
    check(request('GET','/api/orders?buyerId='+me['userId'],token=user),200,'order route','order-service')
    check(request('GET','/api/admin/users',token=user),403,'4. USER cannot access ADMIN endpoint')
    data=check(request('GET','/api/admin/users',token=admin),200,'ADMIN access allowed','user-service')
    assert all('passwordHash' not in row and 'password' not in row for row in data)
    forged=check(request('GET','/api/auth/me',token=user,headers={'X-User-Id':'forged','X-User-Roles':'ADMIN'}),200,'forged identity headers overwritten','user-service')
    assert forged['forwardedUserId']==me['userId'] and forged['forwardedRoles']=='USER'
    check(request('GET','/api/admin/users',token=user,headers={'X-User-Roles':'ADMIN'}),403,'forged role cannot escalate privileges')
    parts=user.split('.');parts[2]=('A' if parts[2][0]!='A' else 'B')+parts[2][1:]
    check(request('GET','/api/items',token='.'.join(parts)),401,'tampered JWT rejected')
    # Generate deliberately invalid fixtures with the local test key; never print them.
    import base64
    from cryptography.hazmat.primitives import serialization, hashes
    from cryptography.hazmat.primitives.asymmetric import padding
    key=serialization.load_der_private_key(base64.b64decode(cfg['JWT_PRIVATE_KEY']),password=None)
    def b64(raw):return base64.urlsafe_b64encode(raw).rstrip(b'=').decode()
    def invalid_jwt(issuer,expires):
        head=b64(b'{"alg":"RS256"}')
        claims=b64(json.dumps({'iss':issuer,'sub':me['userId'],'roles':['ADMIN'],'exp':expires}).encode())
        payload=head+'.'+claims
        return payload+'.'+b64(key.sign(payload.encode(),padding.PKCS1v15(),hashes.SHA256()))
    check(request('GET','/api/items',token=invalid_jwt('campustrade-user-service',int(time.time())-300)),401,'expired JWT rejected')
    check(request('GET','/api/items',token=invalid_jwt('untrusted-issuer',int(time.time())+300)),401,'wrong issuer rejected')
    item=check(request('POST','/api/items',{'sellerId':admin_me['userId'],'title':'Auth-'+str(time.time_ns()),'description':'Week09 verification','price':12.5},admin),201,'publish item with JWT relay','product-service')
    check(request('POST','/api/orders',{'itemId':item['id'],'buyerId':admin_me['userId']},user),403,'buyer identity spoof blocked')
    order=check(request('POST','/api/orders',{'itemId':item['id'],'buyerId':me['userId']},user),201,'order -> Feign -> product -> user','order-service')
    check(request('POST','/api/orders/'+order['id']+'/cancel',token=user),200,'cancel -> Feign releases item','order-service')
    # Direct container access has no public port, and still requires a verified JWT.
    raw=subprocess.run(['docker','compose','exec','-T','user-service','curl','-s','-o','/dev/null','-w','%{http_code}','http://localhost:8081/api/auth/me'],cwd=ROOT,capture_output=True,text=True,check=True)
    assert raw.stdout.strip()=='401','Downstream accepts unauthenticated direct request'
    print('PASS direct downstream request without JWT: HTTP 401',flush=True)
    config=json.loads(subprocess.run(['docker','compose','config','--format','json'],cwd=ROOT,capture_output=True,text=True,check=True).stdout)
    gateway_config=json.loads(config['services']['gateway-service']['environment']['SPRING_APPLICATION_JSON'])
    routes=gateway_config['spring']['cloud']['gateway']['server']['webflux']['routes']
    assert len(routes)>=3 and {r['uri'] for r in routes}=={'lb://user-service','lb://product-service','lb://order-service'},'Compose gateway service-name routes are incomplete'
    print('PASS root compose configures all three service-name routes',flush=True)
    for name in ['user-service','product-service','product-service-2','order-service']:
        assert not config['services'][name].get('ports'),name+' publishes a bypass port'
    print('PASS downstream business ports are not published',flush=True)
    print('PASS ALL: routes, login, 401/403, role checks, identity headers, JWT relay and downstream protection.',flush=True)
if __name__=='__main__':
    try:main()
    except Exception as e:
        # Do not dump request/response objects that could contain a token.
        print('FAIL: '+str(e),file=sys.stderr);sys.exit(1)
