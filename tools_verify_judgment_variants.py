# -*- coding: utf-8 -*-
"""校验：JSON 可解析、变体键齐全、%s 个数与代码里 Arrays.copyOf 的长度一致。"""
import json
import os
import re

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                    "src", "main", "resources", "assets", "jujutsukaisenzone", "lang")

EXPECT = {
    "skill.jujutsukaisenzone.judgment_gavel.desc_no_sentencing": 3,
    "skill.jujutsukaisenzone.judgment_gavel.brief_no_sentencing": 0,
    "skill.jujutsukaisenzone.judgment_trial.desc_no_sentencing": 4,
    "skill.jujutsukaisenzone.judgment_trial.desc_no_aggravation": 6,
    "skill.jujutsukaisenzone.judgment_trial.brief_no_sentencing": 0,
    "skill.jujutsukaisenzone.judgment_accuse.desc_no_sentencing": 4,
    "skill.jujutsukaisenzone.judgment_accuse.desc_no_aggravation": 5,
}

for name in ("zh_cn.json", "en_us.json"):
    with open(os.path.join(BASE, name), "rb") as f:
        raw = f.read()
    eol = "\r\n" if b"\r\n" in raw else "\n"
    data = json.loads(raw.decode("utf-8-sig"))
    bad = 0
    for key, want in EXPECT.items():
        if key not in data:
            print("  MISSING %s :: %s" % (name, key))
            bad += 1
            continue
        got = data[key].count("%s")
        flag = "ok " if got == want else "BAD"
        if got != want:
            bad += 1
        print("  %s %-64s %%s=%d want=%d" % (flag, key, got, want))
    mixed = len([1 for line in raw.decode("utf-8").split("\n") if line.endswith("\r")])
    total = len(raw.decode("utf-8").split("\n")) - 1
    print("%s: eol=%r crlf_lines=%d/%d bad=%d" % (name, eol, mixed, total, bad))
