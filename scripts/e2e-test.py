"""Real HTTP verification; fresh users/products per run. Never print credentials."""
import collections,json,re,subprocess,sys,time
from http.client import HTTPException
from urllib.request import Request,urlopen
from urllib.error import HTTPError,URLError

def call(method,url,body=None,status=200):
    req=Request(url,data=None if body is None else json.dumps(body).encode(),method=method,
                headers={'Content-Type':'application/json'})
    try:
        with urlopen(req,timeout=15) as r: code=r.status; headers=r.headers; raw=r.read()
    except HTTPError as e: code=e.code; headers=e.headers; raw=e.read()
    result=json.loads(raw) if raw else None
    print(method,url,'-> HTTP',code,json.dumps(result,ensure_ascii=False))
    assert code==status,(code,status,result)
    return result,headers

def ready(port):
    for _ in range(90):
        try:
            body,_=call('GET',f'http://localhost:{port}/actuator/health')
            if body['status']=='UP':return
        except (URLError,TimeoutError,AssertionError,HTTPException,ConnectionError):time.sleep(2)
    raise AssertionError(f'Not healthy: {port}')

def command(*args):subprocess.run(args,check=True)

def main():
    for port in [8081,8082,8083,8084]:ready(port)
    for _ in range(60):
        registry=subprocess.check_output([sys.executable,'scripts/nacos-admin.py','list'],text=True)
        if re.search(r'^product-service group=.* healthy=2$',registry,re.M):
            print(registry); break
        time.sleep(2)
    else: raise AssertionError('Expected two healthy product-service instances')
    user='http://localhost:8081/api/users'; product='http://localhost:8082/api/items'; orders='http://localhost:8083/api/orders'
    seller=call('POST',user,{'displayName':'e2e-seller'},201)[0]['id']
    buyer=call('POST',user,{'displayName':'e2e-buyer'},201)[0]['id']
    def item():return call('POST',product,{'sellerId':seller,'title':'Feign Book','description':'week08','price':12.50},201)[0]['id']
    def count():return len(call('GET',orders+'?buyerId='+buyer)[0])
    hits=collections.Counter()
    reservations=[]
    for _ in range(12):
        iid=item(); body,headers=call('POST',orders,{'itemId':iid,'buyerId':buyer},201)
        instance=headers.get('X-Product-Instance'); assert instance in ('product-a','product-b'),instance
        hits[instance]+=1; print('FEIGN DISPATCH',instance)
        assert body['status']=='CREATED' and float(body['amount'])==12.50
        assert call('GET',product+'/'+iid)[0]['status']=='RESERVED'
        reservations.append((body['id'],iid))
    assert set(hits)=={'product-a','product-b'},hits
    print('PASS: actual reservation distribution',dict(hits))
    for oid,iid in reservations:
        assert call('POST',orders+'/'+oid+'/cancel',{},200)[0]['status']=='CANCELLED'
        assert call('GET',product+'/'+iid)[0]['status']=='ON_SALE'
    iid=item(); oid=call('POST',orders,{'itemId':iid,'buyerId':buyer},201)[0]['id']
    call('POST',orders,{'itemId':iid,'buyerId':buyer},409)
    assert call('POST',orders+'/'+oid+'/complete',{},200)[0]['status']=='COMPLETED'
    assert call('GET',product+'/'+iid)[0]['status']=='SOLD'
    before=count()
    call('POST',orders,{'itemId':'missing-product','buyerId':buyer},404)
    assert count()==before
    outage_item=item()
    try:
        command('docker','compose','stop','product-service','product-service-2')
        command(sys.executable,'scripts/nacos-admin.py','wait-down','product-service')
        call('POST',orders,{'itemId':outage_item,'buyerId':buyer},503)
        assert count()==before
        print('PASS: dependency unavailable; order count unchanged',before)
    finally:
        command('docker','compose','start','product-service','product-service-2')
    ready(8082);ready(8084)
    command(sys.executable,'scripts/nacos-admin.py','wait-up','product-service')
    assert call('GET',product+'/'+outage_item)[0]['status']=='ON_SALE'
    call('POST',orders,{'itemId':outage_item,'buyerId':buyer},201)
    assert count()==before+1
    print('PASS: Feign workflow, two instances, 404/409/503, recovery')

if __name__=='__main__':main()
