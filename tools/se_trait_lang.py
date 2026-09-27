"""SE 术式侵入改造：给 zh_cn / en_us 同步增删 lang 键。

两个 lang 文件的换行风格要靠探测（io.open(..., newline='')），
这样插入 / 删除后不会把整文件换行改写掉（否则 diff 会炸）。
每步都校验：JSON 仍可解析、且行数变化符合预期。
"""
import io
import json

LANG_DIR = (r"C:\D\MineMOD\JuJutsuKaisenZone_forge-1.20.1"
            r"\src\main\resources\assets\jujutsukaisenzone\lang")

# 键名统一前缀
SE = "gui.jujutsukaisenzone.se."
SP = "gui.jujutsukaisenzone.sp_node."
PD = "gui.jujutsukaisenzone.player_data."
TR = "gui.jujutsukaisenzone.trait."
PS = "gui.jujutsukaisenzone.panel_stat."
MSG = "message.jujutsukaisenzone."

# 1) 直接删除的键（槽位模型被「已装载集合」取代）
REMOVE = [
    SE + "section.slots",
    SE + "slot",
    SE + "empty",
    SE + "hint.slot",
    MSG + "se.equip.bad_slot",
    MSG + "se.unequip.bad_slot",
]

# 2) 就地改名 + 改文案：旧键 -> (新键, zh, en)
RENAME = {
    SP + "header.slots": (
        SP + "header.quality",
        "\u8d28\u91cf %s / %s\uff08\u5bbd\u9650 +%s\uff09",
        "Quality %s / %s (allowance +%s)",
    ),
}

# 3) 追加：锚点键 -> 追加位置之后
INSERT_AFTER = [
    (SE + "title", [
        (TR + "refined", "\u7cbe", "Refined"),
        (TR + "coarse", "\u7c97", "Coarse"),
        (TR + "attributed", "\u5c5e", "Attributed"),
        (TR + "normal", "\u5e38", "Normal"),
        (PS + "physical_power", "\u4f53\u672f\u5f3a\u5ea6", "Physical Power"),
        (PS + "technique_power", "\u672f\u5f0f\u5f3a\u5ea6", "Technique Power"),
        (PS + "physical_resistance", "\u4f53\u672f\u6297\u6027", "Physical Resistance"),
        (PS + "technique_resistance", "\u672f\u5f0f\u6297\u6027", "Technique Resistance"),
        (PS + "physical_penetration", "\u7a7f\u7532", "Physical Penetration"),
        (PS + "technique_penetration", "\u672f\u7a7f", "Technique Penetration"),
        (PS + "reverse_technique_efficiency", "\u53cd\u8f6c\u672f\u5f0f\u6548\u7387",
         "Reverse Technique Efficiency"),
        (PS + "domain_gauge_charge_efficiency", "\u9886\u57df\u5145\u80fd\u6548\u7387",
         "Domain Gauge Charge Efficiency"),
        (PS + "crit_chance", "\u66b4\u51fb\u51e0\u7387", "Crit Chance"),
    ]),
    (SE + "effect.fallback", [
        (SE + "section.trait", "\u5492\u529b\u7279\u8d28", "Cursed Energy Trait"),
        (SE + "trait.value", "\u5f53\u524d\uff1a%s", "Current: %s"),
        (SE + "trait.any", "\u4e0d\u9650\u7279\u8d28", "Any trait"),
        (SE + "quality.value", "\u8d28\u91cf %s / %s\uff08\u5bbd\u9650 +%s\uff09",
         "Quality %s / %s (allowance +%s)"),
        (SE + "quality.overload", "\u8d85\u8f7d %s", "Overload %s"),
        (SE + "section.equipped", "\u5df2\u88c5\u8f7d", "Equipped"),
        (SE + "equipped.empty", "\u672a\u88c5\u8f7d\u4efb\u4f55 SE", "No SE equipped"),
        (SE + "penalty.line", "\u8d28\u91cf %s \u00b7 \u4ee3\u4ef7\uff1a%s \u2212%s",
         "Quality %s \u00b7 Cost: %s \u2212%s"),
        (SE + "hint.equipped", "\u70b9\u5df2\u88c5\u8f7d\u6761\u76ee\u5378\u4e0b",
         "Click an equipped entry to remove it"),
        (SE + "requirement", "\u9700 %s \u00b7 %s \u00b7 \u8d28\u91cf %s",
         "Requires %s \u00b7 %s \u00b7 Quality %s"),
        (SE + "scope.generic", "\u901a\u7528", "Generic"),
        (SE + "scope.technique", "\u4e13\u7528\uff1a%s", "Restricted: %s"),
        (SE + "status.trait_mismatch", "\u7279\u8d28\u4e0d\u7b26", "Trait mismatch"),
        (SE + "status.technique_mismatch", "\u975e\u672c\u672f\u5f0f\u4e13\u7528",
         "Wrong technique"),
        (SE + "status.quality_exceeded", "\u8d28\u91cf\u4e0d\u8db3", "Not enough quality"),
    ]),
    (MSG + "se.equip.missing_item", [
        (MSG + "se.equip.trait_mismatch", "\u5492\u529b\u7279\u8d28\u4e0d\u7b26\uff0c\u65e0\u6cd5\u4fb5\u5165",
         "Cursed energy trait mismatch \u2014 cannot apply SE"),
        (MSG + "se.equip.technique_mismatch", "\u8be5 SE \u4ec5\u9650\u6307\u5b9a\u672f\u5f0f",
         "This SE is restricted to a specific technique"),
        (MSG + "se.equip.quality_exceeded", "\u8d28\u91cf\u4e0d\u8db3\uff0c\u65e0\u6cd5\u518d\u88c5\u8f7d",
         "Not enough quality to equip"),
    ]),
    (PD + "heavenly_restriction", [
        (PD + "cursed_energy_trait", "\u5492\u529b\u7279\u8d28", "Cursed Energy Trait"),
    ]),
]

# 4) 追加：锚点键之前（锚点恰好是文件最后一项，插到它后面会多出尾逗号）
INSERT_BEFORE = [
    (MSG + "se.unequip.empty_slot", [
        (MSG + "se.unequip.not_equipped", "\u8be5 SE \u672a\u88c5\u8f7d", "That SE is not equipped"),
    ]),
]


def key_index(lines, key):
    needle = '"%s":' % key
    for i, line in enumerate(lines):
        if line.lstrip().startswith(needle):
            return i
    raise KeyError(key)


def indent_of(line):
    return line[:len(line) - len(line.lstrip())]


def render(indent, key, value):
    return '%s"%s": %s,' % (indent, key, json.dumps(value, ensure_ascii=False))


for file_name, lang in (("zh_cn.json", 1), ("en_us.json", 2)):
    path = LANG_DIR + "\\" + file_name
    with io.open(path, "r", encoding="utf-8", newline="") as handle:
        raw = handle.read()

    eol = "\r\n" if "\r\n" in raw else "\n"
    lines = raw.split(eol)
    changes = 0

    for key in REMOVE:
        try:
            i = key_index(lines, key)
        except KeyError:
            continue
        del lines[i]
        changes -= 1

    for old_key, (new_key, zh, en) in RENAME.items():
        value = zh if lang == 1 else en
        try:
            i = key_index(lines, old_key)
        except KeyError:
            continue
        lines[i] = render(indent_of(lines[i]), new_key, value)
        changes += 0

    for anchor, entries in INSERT_AFTER:
        i = key_index(lines, anchor)
        if not lines[i].rstrip().endswith(","):
            lines[i] = lines[i].rstrip() + ","
        indent = indent_of(lines[i])
        new_lines = []
        for key, zh, en in entries:
            if any(('"%s":' % key) in line for line in lines):
                continue
            new_lines.append(render(indent, key, zh if lang == 1 else en))
        lines[i + 1:i + 1] = new_lines
        changes += len(new_lines)

    for anchor, entries in INSERT_BEFORE:
        i = key_index(lines, anchor)
        indent = indent_of(lines[i])
        new_lines = []
        for key, zh, en in entries:
            if any(('"%s":' % key) in line for line in lines):
                continue
            new_lines.append(render(indent, key, zh if lang == 1 else en))
        lines[i:i] = new_lines
        changes += len(new_lines)

    updated = eol.join(lines)
    json.loads(updated)
    with io.open(path, "w", encoding="utf-8", newline="") as handle:
        handle.write(updated)
    print("%s: eol=%r keys=%d" % (file_name, eol, len(json.loads(updated))))
