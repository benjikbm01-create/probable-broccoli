"""Builds the game's data files from the story sources. Run: python3 tools/content/build.py"""
import json, os, sys, importlib.util
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
OUT = os.path.join(HERE, "..", "..", "mod", "src", "main", "resources", "data", "forgotten_island", "fi")
from quests import OBJECTIVES
from people import PEOPLE, SECRETS
from books import BOOKS
from schedules import SCHEDULES, CHATS

def load(name):
    spec = importlib.util.spec_from_file_location(name, os.path.join(HERE, "chars", name + ".py"))
    m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m); return m.d

os.makedirs(os.path.join(OUT, "dialogue"), exist_ok=True)
chars = ["hester", "tobias", "wendel", "corin", "wren", "ione", "sol", "echo", "warden"]
nodes = 0
for c in chars:
    d = load(c)
    nodes += len(d.nodes)
    # sanity: every target exists
    for nid, n in d.nodes.items():
        targets = [n.get("to")] + [o["to"] for o in n.get("options", [])]
        for t in targets:
            if t and t != "end" and t not in d.nodes: raise SystemExit(f"{c}.{nid}: missing target {t}")
    with open(os.path.join(OUT, "dialogue", c + ".json"), "w") as f: json.dump(d.json(), f, indent=1, ensure_ascii=False)
with open(os.path.join(OUT, "quests.json"), "w") as f: json.dump({"objectives": OBJECTIVES}, f, indent=1, ensure_ascii=False)
with open(os.path.join(OUT, "people.json"), "w") as f: json.dump({"people": PEOPLE, "secrets": SECRETS}, f, indent=1, ensure_ascii=False)
with open(os.path.join(OUT, "books.json"), "w") as f: json.dump(BOOKS, f, indent=1, ensure_ascii=False)
with open(os.path.join(OUT, "schedules.json"), "w") as f: json.dump({"npcs": SCHEDULES, "chats": CHATS}, f, indent=1, ensure_ascii=False)
print(f"content: {len(chars)} dialogue files, {nodes} nodes, {len(OBJECTIVES)} objectives, {len(BOOKS)} books")
