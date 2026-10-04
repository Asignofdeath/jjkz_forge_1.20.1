"""Patch liaison office NBT for standalone near-village structure (no street expansion)."""
from pathlib import Path
import nbtlib
from nbtlib import Compound, String, Int, List

ROOT = Path(r"c:\D\MineMOD\JuJutsuKaisenZone_forge-1.20.1")
LIAISON = ROOT / "src/main/resources/data/jujutsukaisenzone/structures/jujutsu_phenomena_council_liaison_office_v4.nbt"


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


def main():
    n = nbtlib.load(LIAISON, gzipped=True)
    sx, sy, sz = [int(v) for v in n["size"]]
    lx, ly, lz = 0, 1, sz // 2
    # Standalone start piece: no further jigsaw expansion
    set_jigsaw_block(
        n, lx, ly, lz, "west_up",
        name="minecraft:building_entrance",
        pool="minecraft:empty",
        target="minecraft:building_entrance",
        final_state="minecraft:structure_void",
        joint="aligned",
    )
    n.save(LIAISON, gzipped=True)
    print(f"liaison standalone jigsaw at ({lx},{ly},{lz}) west_up size=({sx},{sy},{sz})")
    print("done")


if __name__ == "__main__":
    main()
