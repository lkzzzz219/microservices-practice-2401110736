"""Prepare local RSA keys and random demo passwords; never display private values."""
from pathlib import Path
import base64, hashlib, os, secrets
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives import serialization

ROOT=Path(__file__).resolve().parents[1]
def config():
    values={}
    if (ROOT/'.env').exists():
        for line in (ROOT/'.env').read_text(encoding='utf-8-sig').splitlines():
            if '=' in line and not line.lstrip().startswith('#'):
                key,value=line.split('=',1);values[key.strip()]=value.strip().strip('"').strip("'")
    values.update(os.environ)
    return values
def main():
    path=ROOT/'.env';text=path.read_text(encoding='utf-8-sig') if path.exists() else ''
    cfg=config();new={}
    if bool(cfg.get('JWT_PRIVATE_KEY')) != bool(cfg.get('JWT_PUBLIC_KEY')):
        raise RuntimeError('Incomplete key pair in .env; keep both matching keys')
    if not cfg.get('JWT_PRIVATE_KEY'):
        key=rsa.generate_private_key(public_exponent=65537,key_size=2048)
        new['JWT_PRIVATE_KEY']=base64.b64encode(key.private_bytes(serialization.Encoding.DER,serialization.PrivateFormat.PKCS8,serialization.NoEncryption())).decode()
        new['JWT_PUBLIC_KEY']=base64.b64encode(key.public_key().public_bytes(serialization.Encoding.DER,serialization.PublicFormat.SubjectPublicKeyInfo)).decode()
    for role in ['USER','ADMIN']:
        password_key='AUTH_'+role+'_PASSWORD';hash_key='AUTH_'+role+'_HASH'
        if not cfg.get(password_key):new[password_key]=secrets.token_urlsafe(20)
        if not cfg.get(hash_key):
            salt=secrets.token_bytes(16)
            password=cfg.get(password_key,new.get(password_key))
            new[hash_key]=(salt+hashlib.pbkdf2_hmac('sha256',password.encode(),salt,600000,dklen=32)).hex()
    for key,value in new.items():
        if key in cfg:raise RuntimeError('Empty/placeholder value for '+key+'; correct local .env')
        text+='\n'+key+'='+value
    path.write_text(text.rstrip()+'\n',encoding='utf-8',newline='\n')
    print('PASS: local keys and salted password hashes prepared in ignored .env; existing values preserved.')
if __name__=='__main__':main()
