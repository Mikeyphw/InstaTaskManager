#!/usr/bin/env python3
"""Emit minimal JVM-only IG455 shape mocks; no downloaded app code."""
from pathlib import Path
import struct,sys
out=Path(sys.argv[1]);out.mkdir(parents=True,exist_ok=True)
def u2(v):return struct.pack('>H',v)
def u4(v):return struct.pack('>I',v)
class Pool:
    def __init__(self):self.items=[];self.memo={}
    def add(self,tag,data):
        key=(tag,data)
        if key in self.memo:return self.memo[key]
        self.items.append(bytes([tag])+data)
        idx=len(self.items);self.memo[key]=idx;return idx
    def utf(self,s):
        b=s.encode('utf-8');return self.add(1,u2(len(b))+b)
    def cls(self,n):return self.add(7,u2(self.utf(n)))
    def name_type(self,n,d):return self.add(12,u2(self.utf(n))+u2(self.utf(d)))
    def methodref(self,c,n,d):return self.add(10,u2(self.cls(c))+u2(self.name_type(n,d)))

def emit(name,fields=(),iface=(),interface=False):
    p=Pool();this=p.cls(name);super_=p.cls('java/lang/Object');interfaces=[p.cls(x) for x in iface]
    attributes=p.utf('Code');init=p.utf('<init>');void=p.utf('()V');su_init=p.methodref('java/lang/Object','<init>','()V')
    entries=[(access,p.utf(f),p.utf(t)) for access,f,t in fields]
    body=b'\xca\xfe\xba\xbe'+u2(0)+u2(52)
    body+=u2(len(p.items)+1)+b''.join(p.items)
    body+=u2(0x0601 if interface else 0x0021)+u2(this)+u2(super_)
    body+=u2(len(interfaces))+b''.join(map(u2,interfaces))
    body+=u2(len(entries))
    for access,fn,ft in entries:body+=u2(access)+u2(fn)+u2(ft)+u2(0)
    if interface:
        body+=u2(0)
    else:
        code=bytes([0x2a,0xb7])+u2(su_init)+bytes([0xb1])
        dat=u2(1)+u2(1)+u4(len(code))+code+u2(0)+u2(0)
        body+=u2(1)+u2(0x0001)+u2(init)+u2(void)+u2(1)+u2(attributes)+u4(len(dat))+dat
    body+=u2(0)
    pth=out/(name+'.class');pth.parent.mkdir(parents=True,exist_ok=True);pth.write_bytes(body)

emit('X/0Nhe', interface=True)
emit('X/0Ngy', interface=True)
emit('X/00w8')
emit('X/09ZB')
emit('X/09j4',fields=[(0x0019,'A00','LX/09j4;')],iface=['X/0Nhe'])
emit('X/09Xx',fields=[(0x0019,'A00','LX/09Xx;')],iface=['X/0Ngy'])
emit('X/09YD',fields=[(0x0001,'A00','I')],iface=['X/0Ngy'])
emit('X/09ZC',fields=[(0x0001,'A00','I'),(0x0001,'A01','LX/00w8;'),
    (0x0001,'A02','LX/09ZB;'),(0x0001,'A03','Ljava/lang/String;'),
    (0x0001,'A04','Ljava/lang/String;'),(0x0001,'A05','Ljava/lang/String;'),
    (0x0001,'A06','Z'),(0x0001,'A07','Z')],iface=['X/0Nhe'])
print('ITM_P04_HF02_SYNTHETIC_JVM_SHAPES_GENERATED=8')
