"""全工程 javac 静态检查：不跑 gradlew、不写 build/，classpath 从 gradle 缓存现场拼。

机器无关：PROJECT 取本脚本所在项目的根目录；gradle 缓存取当前用户 ~\\.gradle。
classpath 两条铁律（2026-10-24 修噪音）：
  1. 含 .java 条目的 jar 一律跳过（ForgeGradle deobf 依赖可能混进纯源码 jar，
     javac 会隐式编译它们，把依赖模组内部访问 MC 私有成员的错全算到本工程头上）。
  2. 同一构件（artifact）只留版本最新的一份（新旧 LDLib 同 classpath 时
     javac 认旧版，会出现「新版才有方法」的假性找不到符号）。

输出是 GBK，必须按 GBK 解码后再统计（否则 grep 中文关键字会静默失败）。
"""
import collections
import os
import re
import subprocess
import sys
import tempfile
import zipfile

PROJECT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GRADLE_HOME = os.path.join(os.path.expanduser('~'), '.gradle')
ROOTS = [os.path.join(GRADLE_HOME, 'caches', 'forge_gradle'),
         os.path.join(GRADLE_HOME, 'caches', 'modules-2', 'files-2.1')]
KEEP_GLOB = os.path.join(GRADLE_HOME, 'caches', 'forge_gradle', 'minecraft_user_repo',
                         'net', 'minecraftforge', 'forge',
                         '*', 'forge-*_mapped_official_1.20.1.jar')
TMP = tempfile.gettempdir()


def version_sort_key(ver):
    """从版本段提取可排序 key：'7809449_mapped_official_1.20.1' ->
    ((7809449,), official) ；'20.14.16-mc1.20.1-forge_mapped_...' -> ((20,14,16,1,20,1,forge), official)。"""
    mapping = 1 if '_mapped_official' in ver else 0
    core = ver.split('_mapped')[0]
    parts = re.findall(r'[A-Za-z]+|\d+', core)
    key = tuple(int(p) if p.isdigit() else p for p in parts)
    # 混元组比较：统一转成 (int|str) 序列，数字段前加 0 / 字符串段前加 1 保证可比较
    norm = tuple((0, p) if isinstance(p, int) else (1, p) for p in key)
    return (norm, mapping)


def artifact_key(path):
    """仅对 deobf_dependencies 内的 jar 返回 (artifact, version)；其余路径 → None。
    结构：deobf_dependencies/<flavor>/<maven>/<artifact>/<version _mapped ...>/<file>"""
    norm = path.replace('\\', '/')
    if 'deobf_dependencies/' not in norm:
        return None
    rel = norm.split('deobf_dependencies/')[1].split('/')
    if len(rel) >= 4:
        return rel[-3], rel[-2]
    return None


def jar_has_sources(path):
    try:
        with zipfile.ZipFile(path) as z:
            for n in z.namelist():
                if n.endswith('.java'):
                    return True
    except Exception:
        pass
    return False


def contains_minecraft(path):
    try:
        return 'net/minecraft/network/chat/Component.class' in zipfile.ZipFile(path).namelist()
    except Exception:
        return False


keep = [p for p in __import__('glob').glob(KEEP_GLOB)]
if not keep:
    print('!! 找不到 forge mapped jar：', KEEP_GLOB)
    sys.exit(2)

candidates = [keep[0]]
for root in ROOTS:
    for dp, dn, fn in os.walk(root):
        for f in fn:
            if not f.endswith('.jar') or '-sources' in f or '-javadoc' in f:
                continue
            p = os.path.join(dp, f)
            if os.path.normcase(p) == os.path.normcase(keep[0]):
                continue
            if 'mergeMappings' in p or os.path.getsize(p) < 2000:
                continue
            candidates.append(p)

# 过滤：deobf_dependencies 内（1）含 .java 的纯源码 jar 跳过；（2）同构件只留最新版（official 优先）。
# 其余缓存（modules-2、minecraft_user_repo 等）结构不可靠，原样全收。
best = {}
tail = []
for p in candidates:
    key = artifact_key(p)
    if key is None:
        if contains_minecraft(p):
            continue
        tail.append(p)
        continue
    if jar_has_sources(p):
        continue
    art, ver = key
    k = version_sort_key(ver)
    cur = best.get(art)
    if cur is None or k > cur[0]:
        best[art] = (k, p)
jars = [keep[0]] + [v[1] for v in best.values()] + tail

src_root = os.path.join(PROJECT, 'src', 'main', 'java')
srcs = [os.path.join(dp, f).replace('\\', '/')
        for dp, dn, fn in os.walk(src_root) for f in fn if f.endswith('.java')]

argf = os.path.join(TMP, 'jjkz_args.txt')
outdir = os.path.join(TMP, 'jjkz_check_out')
os.makedirs(outdir, exist_ok=True)
with open(argf, 'w', encoding='utf-8') as fh:
    fh.write('-encoding UTF-8\n-proc:none\n-nowarn\n-Xmaxerrs 10000\n')
    fh.write('-d "%s"\n' % outdir.replace('\\', '/'))
    fh.write('-cp "%s"\n' % ';'.join(x.replace('\\', '/') for x in jars))
    for s in srcs:
        fh.write('"%s"\n' % s)

print('srcs =', len(srcs), 'jars =', len(jars), '(候选', len(candidates), ')')
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
if raw.strip() and sum(kinds.values()):
    print('---- 错误原文（前 60 行）----')
    shown = 0
    for line in raw.splitlines():
        if '错误' in line:
            print(line)
            shown += 1
            if shown >= 60:
                break

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
