"""全工程 javac 静态检查：不跑 gradlew、不写 build/，classpath 从 gradle 缓存现场拼。

输出是 GBK，必须按 GBK 解码后再统计（否则 grep 中文关键字会静默失败）。
"""
import collections
import os
import re
import subprocess
import zipfile

PROJECT = r'C:\D\MineMOD\JuJutsuKaisenZone_forge-1.20.1'
KEEP = (r'C:\Users\19316\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge'
        r'\forge\1.20.1-47.4.10_mapped_official_1.20.1'
        r'\forge-1.20.1-47.4.10_mapped_official_1.20.1.jar')
roots = [r'C:\Users\19316\.gradle\caches\forge_gradle',
         r'C:\Users\19316\.gradle\caches\modules-2\files-2.1']

jars = []
for root in roots:
    for dp, dn, fn in os.walk(root):
        for f in fn:
            if not f.endswith('.jar') or '-sources' in f or '-javadoc' in f:
                continue
            p = os.path.join(dp, f)
            if 'mergeMappings' in p or os.path.getsize(p) < 2000:
                continue
            if os.path.normcase(p) == os.path.normcase(KEEP):
                jars.append(p)
                continue
            try:
                if 'net/minecraft/network/chat/Component.class' in zipfile.ZipFile(p).namelist():
                    continue
            except Exception:
                pass
            jars.append(p)
jars = [KEEP] + [j for j in jars if j != KEEP]

src_root = os.path.join(PROJECT, 'src', 'main', 'java')
srcs = [os.path.join(dp, f).replace('\\', '/')
        for dp, dn, fn in os.walk(src_root) for f in fn if f.endswith('.java')]

argf = 'C:/Users/19316/AppData/Local/Temp/jjkz_args.txt'
with open(argf, 'w', encoding='utf-8') as fh:
    fh.write('-encoding UTF-8\n-proc:none\n-nowarn\n-Xmaxerrs 10000\n')
    fh.write('-d "C:/Users/19316/AppData/Local/Temp/jjkz_check_out"\n')
    fh.write('-cp "%s"\n' % ';'.join(x.replace('\\', '/') for x in jars))
    for s in srcs:
        fh.write('"%s"\n' % s)

print('srcs =', len(srcs), 'jars =', len(jars))
p = subprocess.run(['javac', '@' + argf], capture_output=True)
raw = p.stdout.decode('gbk', errors='replace') + p.stderr.decode('gbk', errors='replace')
print('exit =', p.returncode)
kinds = collections.Counter()
files = collections.Counter()
for line in raw.splitlines():
    m = re.search(r'错误[:：]\s*(.*)', line)
    if m:
        kinds[m.group(1).strip()] += 1
    m2 = re.match(r'(.*\.java):(\d+):', line)
    if m2 and '错误' in line:
        files[m2.group(1).replace('\\', '/').split('/')[-1]] += 1
print('错误总行数:', sum(kinds.values()))
for k, v in kinds.most_common(20):
    print(v, '|', k)
for k, v in files.most_common(20):
    print(v, '|', k)
