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

# ---- 守卫：skillCombat 配置常量旁路 ----
# 下面这些常量是 Forge 配置项（config/jujutsukaisenzone-common.toml 的 [skillCombat] 段）的
# 「默认值」，真值要经 SkillCombatConfigRegistry 按咒术等级解析。业务代码直接引用它们
# = 同时丢掉配置修改与等级成长。规则见 SkillCombatConfigRegistry 的类注释。
# 只允许 3 类读者：常量自己的声明文件、Config.java（defineInRange 默认值）、
# SkillCombatConfigRegistry.java（buildCodeDefaults）。
CONFIG_DEFAULTS = {
    'LimitlessBlueConfig.DAMAGE': 'LimitlessBlueConfig.java',
    'LimitlessBlueConfig.DAMAGE_PER_RANK': 'LimitlessBlueConfig.java',
    'LimitlessBlueConfig.PULL_RADIUS': 'LimitlessBlueConfig.java',
    'LimitlessBlueConfig.PULL_RADIUS_PER_RANK': 'LimitlessBlueConfig.java',
    'LimitlessRedConfig.ARROW_EXPLOSION_POWER': 'LimitlessRedConfig.java',
    'LimitlessRedConfig.EXPLOSION_POWER_PER_RANK': 'LimitlessRedConfig.java',
    'LimitlessRedConfig.ARROW_KNOCKBACK_RADIUS': 'LimitlessRedConfig.java',
    'LimitlessRedConfig.KNOCKBACK_RADIUS_PER_RANK': 'LimitlessRedConfig.java',
    'LimitlessRemoteMurasakiConfig.MAX_FLIGHT_CHUNK_COUNT': 'LimitlessRemoteMurasakiConfig.java',
    'LimitlessRemoteMurasakiConfig.MAX_FLIGHT_CHUNK_COUNT_PER_RANK': 'LimitlessRemoteMurasakiConfig.java',
    'ShrineKaiConfig.COLUMN_DAMAGE': 'ShrineKaiConfig.java',
    'ShrineKaiConfig.COLUMN_DAMAGE_PER_RANK': 'ShrineKaiConfig.java',
    'ShrineHachiConfig.DAMAGE': 'ShrineHachiConfig.java',
    'ShrineHachiConfig.DAMAGE_PER_RANK': 'ShrineHachiConfig.java',
    'ShrineDomainSkillGrowth.BASELINE_SLASH_DAMAGE': 'ShrineDomainSkillGrowth.java',
    'ShrineDomainSkillGrowth.BASELINE_SLASH_DAMAGE_PER_RANK': 'ShrineDomainSkillGrowth.java',
    'ShrineDomainSkillGrowth.BASELINE_POWER_SLASH_DAMAGE': 'ShrineDomainSkillGrowth.java',
    'ShrineDomainSkillGrowth.BASELINE_POWER_SLASH_DAMAGE_PER_RANK': 'ShrineDomainSkillGrowth.java',
}
ALLOWED_BYPASS_FILES = {'Config.java', 'SkillCombatConfigRegistry.java'}
# 确实只能读默认值的场合（占位 / 老存档兜底等），在「本行或上一行」写这个标记来豁免，
# 并且必须跟上理由。禁止用它来掩盖「本该走配置解析却图省事」的引用。
BYPASS_MARK = 'jjkz-config-default-ok'
patterns = {k: re.compile(r'\b%s\b' % re.escape(k)) for k in CONFIG_DEFAULTS}
bypass = []
for s in srcs:
    base = os.path.basename(s)
    if base in ALLOWED_BYPASS_FILES:
        continue
    with open(s, encoding='utf-8', errors='replace') as fh:
        window = []          # 最近 3 行，供多行注释里的标记豁免用
        for lineno, line in enumerate(fh.read().splitlines(), 1):
            head = line.lstrip()
            exempt = BYPASS_MARK in line or any(BYPASS_MARK in w for w in window)
            window.append(line)
            del window[:-3]
            if head.startswith('*') or head.startswith('//') or head.startswith('/*'):
                continue
            for name, pat in patterns.items():
                if pat.search(line) and base != CONFIG_DEFAULTS[name] and not exempt:
                    bypass.append('%s:%d  %s' % (base, lineno, name))
print('配置常量旁路(应为 0):', len(bypass))
for b in bypass[:30]:
    print(' ', b)
