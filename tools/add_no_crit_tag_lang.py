"""给 zh_cn / en_us 追加「不参加暴击」标注键。

两个 lang 文件的换行风格不同（zh_cn 是 LF，en_us 是 CRLF），
所以必须 io.open(..., newline='') 读、探测风格后按原风格插入，避免整文件换行被改写。
"""
import io
import json

LANG_DIR = (r"C:\D\MineMOD\JuJutsuKaisenZone_forge-1.20.1"
            r"\src\main\resources\assets\jujutsukaisenzone\lang")
KEY = "gui.jujutsukaisenzone.skill_tree.no_crit_tag"
ANCHOR = "gui.jujutsukaisenzone.skill_tree.domain_expansion_tag"
TARGETS = (
    ("zh_cn.json", "\u4e0d\u53c2\u52a0\u66b4\u51fb"),
    ("en_us.json", "Cannot Crit"),
)

for file_name, value in TARGETS:
    path = LANG_DIR + "\\" + file_name
    with io.open(path, "r", encoding="utf-8", newline="") as handle:
        raw = handle.read()

    if KEY in raw:
        print("%s: key already present, skipped" % file_name)
        continue

    eol = "\r\n" if "\r\n" in raw else "\n"
    anchor_at = raw.index(ANCHOR)
    line_start = raw.rfind(eol, 0, anchor_at) + len(eol)
    line_end = raw.index(eol, anchor_at)
    indent = raw[line_start:anchor_at].split('"')[0]
    new_line = '%s"%s": %s,' % (indent, KEY, json.dumps(value, ensure_ascii=False))
    updated = raw[:line_end + len(eol)] + new_line + eol + raw[line_end + len(eol):]

    # 校验：JSON 仍然可解析，且只多了一行
    json.loads(updated)
    assert updated.count(eol) == raw.count(eol) + 1, file_name
    with io.open(path, "w", encoding="utf-8", newline="") as handle:
        handle.write(updated)
    print("%s: inserted (eol=%r, +1 line)" % (file_name, eol))
