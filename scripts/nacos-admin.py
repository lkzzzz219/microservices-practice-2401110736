"""Local setup / sanitized registry inspection. Never print credentials or tokens."""
from pathlib import Path
import base64, json, os, secrets, sys, time
from urllib.request import Request, urlopen
from urllib.parse import urlencode
from urllib.error import HTTPError, URLError

ROOT = Path(__file__).resolve().parents[1]
def config():
    values = {}
    p = ROOT / '.env'
    if p.exists():
        for line in p.read_text(encoding='utf-8-sig').splitlines():
            if '=' in line and not line.lstrip().startswith('#'):
                k,v=line.split('=',1); values[k.strip()]=v.strip().strip('"').strip("'")
    values.update(os.environ)
    return values

def prepare():
    p=ROOT/'.env'; text=p.read_text(encoding='utf-8-sig') if p.exists() else ''
    values=config()
    defaults={'NACOS_AUTH_TOKEN':base64.b64encode(secrets.token_bytes(48)).decode(),
      'NACOS_AUTH_IDENTITY_KEY':'campustrade-identity', 'NACOS_AUTH_IDENTITY_VALUE':secrets.token_hex(24),
      'NACOS_USERNAME':'nacos','NACOS_PASSWORD':secrets.token_urlsafe(24),'NACOS_GROUP':'PERSONAL_GROUP',
      'NACOS_NAMESPACE':'','NACOS_CONSOLE_PORT':'18080','NACOS_SERVER_PORT':'18849','NACOS_GRPC_PORT':'19849'}
    for k,v in defaults.items():
        if k not in values: text+='\n'+k+'='+v
        elif 'REPLACE_' in values[k]:
            raise RuntimeError('Replace placeholder for '+k+' in local .env before continuing')
    p.write_text(text.rstrip()+'\n',encoding='utf-8',newline='\n')
    print('Private Nacos values prepared in ignored .env; existing values preserved.')

cfg=config()
def request(path, data=None, token=None):
    base='http://127.0.0.1:'+cfg.get('NACOS_SERVER_PORT','18849')+'/nacos'
    headers={'accessToken':token} if token else {}
    req=Request(base+path, data=urlencode(data).encode() if data is not None else None, headers=headers)
    with urlopen(req,timeout=8) as response: result=json.load(response)
    if isinstance(result,dict) and result.get('code',0) not in (0,200):
        raise RuntimeError('Nacos operation rejected (code '+str(result.get('code'))+')')
    return result

def login():
    result=request('/v3/auth/user/login',{'username':cfg.get('NACOS_USERNAME','nacos'),'password':cfg['NACOS_PASSWORD']})
    data=result.get('data',result)
    return data['accessToken']

def registry(token):
    query=urlencode({'pageNo':1,'pageSize':100,'groupName':cfg.get('NACOS_GROUP','PERSONAL_GROUP'),
                     'namespaceId':cfg.get('NACOS_NAMESPACE') or 'public'})
    return request('/v3/admin/ns/service/list?'+query,token=token)['data']['pageItems']

def main():
    command=sys.argv[1] if len(sys.argv)>1 else 'list'
    if command=='prepare': prepare(); return
    if command=='init':
        for attempt in range(60):
            try:
                try: login()
                except (HTTPError,KeyError,RuntimeError):
                    request('/v3/auth/user/admin',{'password':cfg['NACOS_PASSWORD']})
                    login()
                print('Nacos administrator initialized / login verified; credentials were not printed.')
                return
            except (HTTPError,URLError,TimeoutError,ConnectionError,RuntimeError):
                if attempt==59: raise RuntimeError('Initialization failed. Check Nacos and local .env; an existing admin password is not reset.') from None
                time.sleep(2)
    if command.startswith('wait-'):
        name=sys.argv[2]; wanted=command=='wait-up'
        for attempt in range(60):
            token=login(); rows=registry(token)
            count=sum(x.get('healthyInstanceCount',0) for x in rows if x['name']==name)
            if (count>0)==wanted:
                print(f'REGISTRY {name}: healthy instances={count}'); return
            time.sleep(2)
        raise RuntimeError('Registry transition timed out: '+name)
    token=login()
    for row in registry(token):
        q=urlencode({'serviceName':row['name'],'groupName':row['groupName'],
                     'namespaceId':cfg.get('NACOS_NAMESPACE') or 'public'})
        data=request('/v3/admin/ns/instance/list?'+q,token=token)['data']
        hosts=data.get('hosts',[]) if isinstance(data,dict) else data
        print(f"{row['name']} group={row['groupName']} healthy={row.get('healthyInstanceCount',0)}")
        for host in hosts: print(f"  {host['ip']}:{host['port']} healthy={host.get('healthy')}")

if __name__=='__main__':
    try: main()
    except Exception as e:
        # urllib error messages do not include form bodies; never dump response payloads.
        print('ERROR: '+str(e),file=sys.stderr);sys.exit(1)
