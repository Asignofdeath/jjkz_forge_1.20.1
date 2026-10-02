import zipfile
from pathlib import Path
import nbtlib
from nbtlib import Compound, String, Int, List

ROOT = Path(r"c:\D\MineMOD\JuJutsuKaisenZone_forge-1.20.1")
JAR = Path(r"C:\Users\19316\.gradle\caches\forge_gradle\minecraft_repo\versions\1.20.1\client-extra.jar")
TC_OUT = ROOT / "src/main/resources/data/minecraft/structures/village/plains/town_centers"
TC_OUT.mkdir(parents=True, exist_ok=True)

LIAISON = ROOT / "src/main/resources/data/jujutsukaisenzone/structures/jujutsu_phenomena_council_liaison_office_v4.nbt"

POOL = "jujutsukaisenzone:village/plains/liaison_office"
TARGET = "jujutsukaisenzone:liaison_entrance"
CONNECTOR_NAME = "jujutsukaisenzone:liaison_connector"


def ensure_palette_jigsaw(n, orientation):
    for i, p in enumerate(n["palette"]):
        if str(p["Name"]) == "minecraft:jigsaw" and str(p.get("Properties", {}).get("orientation", "")) == orientation:
            return i
    entry = Compound({
        "Name": String("minecraft:jigsaw"),
        "Properties": Compound({"orientation": String(orientation)}),
    })
    n["palette"].append(entry)
    return len(n["palette"]) - 1


def occupied(n, x, y, z):
    for b in n["blocks"]:
        pos = b["pos"]
        if int(pos[0]) == x and int(pos[1]) == y and int(pos[2]) == z:
            return b
    return None


def set_jigsaw_block(n, x, y, z, orientation, name, pool, target, final_state="minecraft:structure_void", joint="aligned"):
    idx = ensure_palette_jigsaw(n, orientation)
    nbt = Compound({
        "id": String("minecraft:jigsaw"),
        "name": String(name),
        "pool": String(pool),
        "target": String(target),
        "final_state": String(final_state),
        "joint": String(joint),
    })
    existing = occupied(n, x, y, z)
    if existing is not None:
        existing["state"] = Int(idx)
        existing["nbt"] = nbt
    else:
        n["blocks"].append(Compound({
            "pos": List[Int]([Int(x), Int(y), Int(z)]),
            "state": Int(idx),
            "nbt": nbt,
        }))


def pick_town_center_attachment(n):
    sx, sy, sz = [int(v) for v in n["size"]]
    candidates = [
        (0, 1, sz // 2, "west_up"),
        (sx - 1, 1, sz // 2, "east_up"),
        (sx // 2, 1, 0, "north_up"),
        (sx // 2, 1, sz - 1, "south_up"),
    ]
    street_pos = set()
    for b in n["blocks"]:
        if "nbt" in b and str(b["nbt"].get("name", "")) == "minecraft:street":
            street_pos.add((int(b["pos"][0]), int(b["pos"][1]), int(b["pos"][2])))
    for x, y, z, ori in candidates:
        if (x, y, z) not in street_pos:
            return x, y, z, ori
    x, y, z, ori = candidates[0]
    if ori == "west_up":
        z = max(0, min(sz - 1, z - 2))
    elif ori == "east_up":
        z = max(0, min(sz - 1, z + 2))
    return x, y, z, ori


def main():
    names = [
        "plains_fountain_01.nbt",
        "plains_meeting_point_1.nbt",
        "plains_meeting_point_2.nbt",
        "plains_meeting_point_3.nbt",
    ]
    with zipfile.ZipFile(JAR) as archive:
        for name in names:
            src = f"data/minecraft/structures/village/plains/town_centers/{name}"
            dest = TC_OUT / name
            dest.write_bytes(archive.read(src))
            n = nbtlib.load(dest, gzipped=True)
            x, y, z, ori = pick_town_center_attachment(n)
            set_jigsaw_block(
                n, x, y, z, ori,
                name=CONNECTOR_NAME,
                pool=POOL,
                target=TARGET,
                final_state="minecraft:structure_void",
                joint="aligned",
            )
            n.save(dest, gzipped=True)
            print(f"town center {name}: jigsaw at ({x},{y},{z}) {ori}")

    n = nbtlib.load(LIAISON, gzipped=True)
    sx, sy, sz = [int(v) for v in n["size"]]
    lx, ly, lz = 0, 1, sz // 2
    set_jigsaw_block(
        n, lx, ly, lz, "west_up",
        name=TARGET,
        pool="minecraft:empty",
        target=TARGET,
        final_state="minecraft:structure_void",
        joint="aligned",
    )
    n.save(LIAISON, gzipped=True)
    print(f"liaison: jigsaw at ({lx},{ly},{lz}) west_up size=({sx},{sy},{sz})")
    print("done")


if __name__ == "__main__":
    main()
