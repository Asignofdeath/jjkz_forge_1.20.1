# -*- coding: utf-8 -*-
"""按解锁状态插入审判术式描述变体文案。保留各文件原有换行风格。"""
import io
import os
import re

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                    "src", "main", "resources", "assets", "jujutsukaisenzone", "lang")

ZH = {
    "skill.jujutsukaisenzone.judgment_gavel.desc": [
        ("skill.jujutsukaisenzone.judgment_gavel.desc_no_sentencing",
         "施展时若背包中没有法槌，则获得一柄法槌；若已有法槌，则切换至下一形态（需解锁「三式法槌」；未解锁时法槌恒为标准形态）。"
         "手持法槌单击右键即可将其射出，标准形态命中造成 %s 术式伤害，落点爆炸强度 %s（术式伤害）且不破坏方块。近战挥击造成体术伤害。"
         "法槌攻击【罪恶者】目标时，伤害提升 %s%%（近战、射击与爆炸均有效）。"
         "\\n\\n目前裁决判【有罪】只会落到【没收】，判不出【死刑】，因此法槌也不会化作【处刑人之剑】——解锁「量刑」后才会。"),
    ],
    "skill.jujutsukaisenzone.judgment_gavel.brief": [
        ("skill.jujutsukaisenzone.judgment_gavel.brief_no_sentencing",
         "随行的槌，也是悬在罪人头顶的刑具。掷出去，落点会炸开；砸在【罪恶者】身上，格外重。"
         "\\n\\n当庭还判不出【死刑】，那柄不留活口的剑也就还不出鞘——要等「量刑」解锁。"),
    ],
    "skill.jujutsukaisenzone.judgment_trial.desc": [
        ("skill.jujutsukaisenzone.judgment_trial.desc_no_sentencing",
         "使用 Epic Fight 锁定一名敌人 %s 秒后发动；蓄力期间施法者受击则被打断。完成蓄力后在身后召唤审判者并裁决："
         "基础 %s%% 判【有罪】，【无罪】则无事发生。目标若处于【罪恶者】，必定【有罪】。"
         "\\n\\n每对该目标完成一次本技能（含【无罪】），下次对其使用本技能时有罪概率 +%s%%（最多 +%s%%）。"
         "\\n\\n判【有罪】时赋予【罪恶者】8 分钟（已有则不刷新），并宣读罪名，随后【没收】。"
         "\\n\\n裁决完成后无论结果，目标获得 15 秒缓慢。完成蓄力后扣除咒力并进入冷却。"
         "\\n\\n解锁「量刑」后，判【有罪】时才有可能落到【死刑】。"),
        ("skill.jujutsukaisenzone.judgment_trial.desc_no_aggravation",
         "使用 Epic Fight 锁定一名敌人 %s 秒后发动；蓄力期间施法者受击则被打断。完成蓄力后在身后召唤审判者并裁决："
         "基础 %s%% 判【有罪】，【无罪】则无事发生。目标若处于【罪恶者】，必定【有罪】。"
         "\\n\\n每对该目标完成一次本技能（含【无罪】），下次对其使用本技能时有罪概率 +%s%%（最多 +%s%%）。"
         "\\n\\n判【有罪】时赋予【罪恶者】8 分钟（已有则不刷新），并宣读罪名。处罚：基础 %s%%【死刑】，否则【没收】；"
         "已判【死刑】时另有 %s%% 变为【附加了没收的死刑】。"
         "\\n\\n解锁「从重论处」后，目标身上的负面效果与罪名轻重还会继续把【死刑】往上推。"
         "\\n\\n裁决完成后无论结果，目标获得 15 秒缓慢。完成蓄力后扣除咒力并进入冷却。"),
    ],
    "skill.jujutsukaisenzone.judgment_trial.brief": [
        ("skill.jujutsukaisenzone.judgment_trial.brief_no_sentencing",
         "你的锁定就是一纸传唤。式神自你身后上前，宣读罪名：有罪者背负【罪恶者】，再受【没收】；无罪者原样离场。"
         "\\n\\n同一个人审得越多，下一次越难脱身。"),
    ],
    "skill.jujutsukaisenzone.judgment_accuse.desc": [
        ("skill.jujutsukaisenzone.judgment_accuse.desc_no_sentencing",
         "以视线锁定目标，唤来随行于身后的审判者，当庭宣读一项随机罪名。被告为玩家时须在 %s 秒内按 1 否认、2 撒谎、3 坦白；"
         "非玩家自动辩护。沉默视同放弃辩护：【没收】、【罪恶者】与 3 秒定身。"
         "\\n\\n证据「罪行确认」基础 %s%%，再加上你对该目标的审判叠层（每完成一次「审判」+%s%%，最多 +%s%%；「指控」不计入叠层）。"
         "\\n\\n裁决结果：\\n· 否认 + 确认 → 【没收】\\n· 否认 + 无法确认 → 无罪\\n· 撒谎 + 确认 → 【没收】+【罪恶者】"
         "\\n· 撒谎 + 无法确认 → 【没收】\\n· 坦白 + 确认 → 【没收】\\n· 坦白 + 无法确认 → 【罪恶者】"
         "\\n\\n解锁「量刑」后，「否认 + 确认」这一支才会掷【死刑】。"),
        ("skill.jujutsukaisenzone.judgment_accuse.desc_no_aggravation",
         "以视线锁定目标，唤来随行于身后的审判者，当庭宣读一项随机罪名。被告为玩家时须在 %s 秒内按 1 否认、2 撒谎、3 坦白；"
         "非玩家自动辩护。沉默视同放弃辩护：【没收】、【罪恶者】与 3 秒定身。"
         "\\n\\n证据「罪行确认」基础 %s%%，再加上你对该目标的审判叠层（每完成一次「审判」+%s%%，最多 +%s%%；「指控」不计入叠层）。"
         "\\n\\n裁决结果：\\n· 否认 + 确认 → 【死刑】或【没收】\\n· 否认 + 无法确认 → 无罪\\n· 撒谎 + 确认 → 【没收】+【罪恶者】"
         "\\n· 撒谎 + 无法确认 → 【没收】\\n· 坦白 + 确认 → 【没收】\\n· 坦白 + 无法确认 → 【罪恶者】"
         "\\n\\n【死刑】基础 %s%%。"
         "\\n\\n解锁「从重论处」后，目标身上的负面效果与罪名轻重还会继续把【死刑】往上推。"),
    ],
}

EN = {
    "skill.jujutsukaisenzone.judgment_gavel.desc": [
        ("skill.jujutsukaisenzone.judgment_gavel.desc_no_sentencing",
         "On cast, grants a Gavel if you don't already carry one; if you already have one, cycles to the next form "
         "(requires the Three-Form Gavel node; without it the Gavel is locked to its Standard form). "
         "Right-click while holding the Gavel to fire it instantly-Standard form direct hit deals %s technique damage; "
         "impact explosion power %s (technique damage) and does not break blocks. Melee swings deal martial damage. "
         "Gavel attacks against Sinful One targets deal %s%% bonus damage (melee, projectile, and explosion)."
         "\\n\\nA Guilty verdict can only end in Confiscation right now: no Death Sentence can be handed down, so the Gavel "
         "cannot become the Executioner's Sword. Unlock Sentencing and it can."),
    ],
    "skill.jujutsukaisenzone.judgment_gavel.brief": [
        ("skill.jujutsukaisenzone.judgment_gavel.brief_no_sentencing",
         "The gavel that walks beside you, and the instrument that waits above every guilty head. "
         "Throw it and the impact bursts; against a Sinful One it bites deeper."
         "\\n\\nYour court cannot pass a Death Sentence yet, so the blade that spares no one stays sheathed-until "
         "Sentencing is unlocked."),
    ],
    "skill.jujutsukaisenzone.judgment_trial.desc": [
        ("skill.jujutsukaisenzone.judgment_trial.desc_no_sentencing",
         "Lock onto an enemy with Epic Fight for %s s; taking damage while charging cancels it. After charging, a Judge "
         "passes judgment: %s%% base Guilty (Innocent does nothing). Sinful One targets are always Guilty."
         "\\n\\nEach completed use of this skill on a target (including Innocent) adds +%s%% guilty chance next time you "
         "use this skill on them (max +%s%%)."
         "\\n\\nOn Guilty: Sinful One for 8 minutes (no refresh), the crime is read aloud, and then Confiscation."
         "\\n\\nAfter the verdict, the target gains 15 s Slowness regardless of outcome. Cursed energy and cooldown apply "
         "when charging completes."
         "\\n\\nUnlock Sentencing and a Guilty verdict may fall on Death Sentence instead."),
        ("skill.jujutsukaisenzone.judgment_trial.desc_no_aggravation",
         "Lock onto an enemy with Epic Fight for %s s; taking damage while charging cancels it. After charging, a Judge "
         "passes judgment: %s%% base Guilty (Innocent does nothing). Sinful One targets are always Guilty."
         "\\n\\nEach completed use of this skill on a target (including Innocent) adds +%s%% guilty chance next time you "
         "use this skill on them (max +%s%%)."
         "\\n\\nOn Guilty: Sinful One for 8 minutes (no refresh) and the crime is read aloud. Punishment: %s%% base Death "
         "Sentence, else Confiscation; if Death Sentence, %s%% chance of Death Sentence with Confiscation."
         "\\n\\nUnlock Aggravation and both the target's afflictions and the weight of the charge will push the Death "
         "Sentence roll higher still."
         "\\n\\nAfter the verdict, the target gains 15 s Slowness regardless of outcome. Cursed energy and cooldown apply "
         "when charging completes."),
    ],
    "skill.jujutsukaisenzone.judgment_trial.brief": [
        ("skill.jujutsukaisenzone.judgment_trial.brief_no_sentencing",
         "Your lock is the summons. The Judge steps up behind you and reads the charge: the Guilty take Sinful One and "
         "then Confiscation, the Innocent walk away untouched."
         "\\n\\nThe more often you judge someone, the harder it gets for them to walk away next time."),
    ],
    "skill.jujutsukaisenzone.judgment_accuse.desc": [
        ("skill.jujutsukaisenzone.judgment_accuse.desc_no_sentencing",
         "Fix your gaze on a target; a Judge follows and reads a random charge. Player defendants have %s s-1 Deny, "
         "2 Lie, 3 Confess; non-players respond automatically. Silence: Confiscation, Sinful One, 3 s immobilization."
         "\\n\\nCrime Confirmed evidence: %s%% base, plus your trial stack on that target (+%s%% per completed Trial on "
         "them, max +%s%%; Accuse does not add stack)."
         "\\n\\nVerdicts-Deny + Confirmed: Confiscation; Deny + Cannot Confirm: acquittal; Lie + Confirmed: Confiscation "
         "and Sinful One; Lie + Cannot Confirm: Confiscation; Confess + Confirmed: Confiscation; "
         "Confess + Cannot Confirm: Sinful One."
         "\\n\\nUnlock Sentencing and the Deny + Confirmed branch rolls for Death Sentence instead."),
        ("skill.jujutsukaisenzone.judgment_accuse.desc_no_aggravation",
         "Fix your gaze on a target; a Judge follows and reads a random charge. Player defendants have %s s-1 Deny, "
         "2 Lie, 3 Confess; non-players respond automatically. Silence: Confiscation, Sinful One, 3 s immobilization."
         "\\n\\nCrime Confirmed evidence: %s%% base, plus your trial stack on that target (+%s%% per completed Trial on "
         "them, max +%s%%; Accuse does not add stack)."
         "\\n\\nVerdicts-Deny + Confirmed: Death Sentence or Confiscation; Deny + Cannot Confirm: acquittal; "
         "Lie + Confirmed: Confiscation and Sinful One; Lie + Cannot Confirm: Confiscation; "
         "Confess + Confirmed: Confiscation; Confess + Cannot Confirm: Sinful One."
         "\\n\\nDeath Sentence base chance: %s%%."
         "\\n\\nUnlock Aggravation and both the target's afflictions and the weight of the charge will push the Death "
         "Sentence roll higher still."),
    ],
}

KEY_RE = re.compile(r'^\s*"([^"]+)"\s*:')


def apply(file_name, inserts):
    path = os.path.join(BASE, file_name)
    with open(path, "rb") as f:
        raw = f.read()
    eol = "\r\n" if b"\r\n" in raw else "\n"
    text = raw.decode("utf-8")
    lines = text.split(eol)

    out = []
    seen = set()
    for line in lines:
        out.append(line)
        m = KEY_RE.match(line)
        if not m:
            continue
        key = m.group(1)
        if key not in inserts:
            continue
        for new_key, new_text in inserts[key]:
            if new_key in seen:
                continue
            seen.add(new_key)
            out.append('  "%s": "%s",' % (new_key, new_text))

    missing = [k for k in inserts if all(nk not in seen for nk, _ in inserts[k])]
    if missing:
        raise SystemExit("anchor not found in %s: %s" % (file_name, missing))

    with open(path, "wb") as f:
        f.write(eol.join(out).encode("utf-8"))
    print("%s: inserted %d keys (eol=%r)" % (file_name, len(seen), eol))


apply("zh_cn.json", ZH)
apply("en_us.json", EN)
