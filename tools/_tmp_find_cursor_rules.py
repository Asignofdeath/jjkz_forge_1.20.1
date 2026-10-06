import json
import sqlite3

p = r"C:\Users\19316\AppData\Roaming\Cursor\User\globalStorage\state.vscdb"
con = sqlite3.connect(p)
cur = con.cursor()
key = "src.vs.platform.reactivestorage.browser.reactiveStorageServiceImpl.persistentStorage.applicationUser"
text = cur.execute("SELECT value FROM ItemTable WHERE key=?", (key,)).fetchone()[0]
if isinstance(text, bytes):
    text = text.decode("utf-8")
obj = json.loads(text)
print("top keys:", list(obj.keys())[:80] if isinstance(obj, dict) else type(obj))
if isinstance(obj, dict):
    for k in obj:
        if any(x in k.lower() for x in ("rule", "prompt", "memory", "agent", "ai")):
            v = obj[k]
            s = str(v)
            print("---", k, "type", type(v).__name__, "len", len(s))
            print(s[:800])
