# -*- coding: utf-8 -*-
from pathlib import Path

root = Path(__file__).resolve().parent / "src/main/resources/assets/jujutsukaisenzone/lang"

item_old = (
    '  "item.jujutsukaisenzone.cursed_object_damaged_leather_watch": "壊れた革の腕時計",\n'
    '  "item.jujutsukaisenzone.cursed_object_melted_ice_pack"'
)
item_new = (
    '  "item.jujutsukaisenzone.cursed_object_damaged_leather_watch": "壊れた革の腕時計",\n'
    '  "item.jujutsukaisenzone.cursed_object_lead_waist_guard": "鉛の入った腰当て",\n'
    '  "item.jujutsukaisenzone.cursed_object_clean_gauze_roll": "きれいなガーゼ",\n'
    '  "item.jujutsukaisenzone.cursed_object_cauterized_cotton": "焦げた止血綿",\n'
    '  "item.jujutsukaisenzone.cursed_object_melted_ice_pack"'
)

zh_effect_old = (
    '  "gui.jujutsukaisenzone.se.effect.damaged_leather_watch": '
    '"造成体术 / 术式暴击时：体术与术式强度各 +5%，持续 5 秒，最高 5 层，叠层刷新时长",\n'
    '  "gui.jujutsukaisenzone.se.effect.fallback"'
)
zh_effect_new = (
    '  "gui.jujutsukaisenzone.se.effect.damaged_leather_watch": '
    '"造成体术 / 术式暴击时：体术与术式强度各 +5%，持续 5 秒，最高 5 层，叠层刷新时长",\n'
    '  "gui.jujutsukaisenzone.se.effect.cut_mark_vital_cap": '
    '"最大生命额外抬高每肢切痕上限（可至软顶 80），高血更耐叠痕",\n'
    '  "gui.jujutsukaisenzone.se.effect.cut_mark_fast_recovery": '
    '"切痕自然恢复加快：每 20 tick 各肢 −2 层（默认 40 tick −1）",\n'
    '  "gui.jujutsukaisenzone.se.effect.dismemberment_bleed_immune": '
    '"断肢后不触发流血；仍缺肢，但不因失血必死",\n'
    '  "gui.jujutsukaisenzone.se.effect.fallback"'
)

en_effect_old = (
    '  "gui.jujutsukaisenzone.se.effect.damaged_leather_watch": '
    '"On martial/technique crit: +5% physical and technique power for 5s, max 5 stacks; '
    'stacking refreshes duration",\n'
    '  "gui.jujutsukaisenzone.se.effect.fallback"'
)
en_effect_new = (
    '  "gui.jujutsukaisenzone.se.effect.damaged_leather_watch": '
    '"On martial/technique crit: +5% physical and technique power for 5s, max 5 stacks; '
    'stacking refreshes duration",\n'
    '  "gui.jujutsukaisenzone.se.effect.cut_mark_vital_cap": '
    '"Max health further raises per-limb cut-mark cap (soft max 80); '
    'tankier builds endure stacking longer",\n'
    '  "gui.jujutsukaisenzone.se.effect.cut_mark_fast_recovery": '
    '"Faster cut-mark decay: -2 per limb every 20 ticks (default -1 every 40)",\n'
    '  "gui.jujutsukaisenzone.se.effect.dismemberment_bleed_immune": '
    '"No bleeding after dismemberment; limb stays missing, but blood loss won\'t finish you",\n'
    '  "gui.jujutsukaisenzone.se.effect.fallback"'
)

patches = {
    "zh_cn.json": (zh_effect_old, zh_effect_new),
    "en_us.json": (en_effect_old, en_effect_new),
}

for name, (eold, enew) in patches.items():
    path = root / name
    text = path.read_text(encoding="utf-8")
    if "cursed_object_lead_waist_guard" not in text:
        if item_old not in text:
            raise SystemExit(f"missing item anchor in {name}")
        text = text.replace(item_old, item_new, 1)
    if "cut_mark_vital_cap" not in text:
        if eold not in text:
            raise SystemExit(f"missing effect anchor in {name}")
        text = text.replace(eold, enew, 1)
    path.write_text(text, encoding="utf-8")
    print("ok", name)
