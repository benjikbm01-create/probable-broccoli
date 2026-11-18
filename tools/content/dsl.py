"""Tiny DSL for authoring dialogue as Python data; build.py writes the game's JSON files."""

class D:
    """One character's dialogue: start entries (first match wins), nodes, barks."""
    def __init__(self, npc):
        self.npc = npc; self.start = []; self.nodes = {}; self.barks = []
    def entry(self, cond, node):
        self.start.append({"if": cond, "node": node} if cond else {"node": node}); return self
    def n(self, id, text, *opts, do=None, to=None, speaker=None):
        assert id not in self.nodes, f"duplicate node {self.npc}.{id}"
        node = {"text": text}
        if speaker: node["speaker"] = speaker
        if do: node["do"] = do
        if to: node["to"] = to
        if opts: node["options"] = [o for o in opts if o is not None]
        self.nodes[id] = node
        return self
    def bark(self, cond, text):
        self.barks.append({"if": cond, "text": text} if cond else {"text": text}); return self
    def json(self):
        return {"npc": self.npc, "start": self.start, "nodes": self.nodes, "barks": self.barks}

def O(text, to="end", cond=None, do=None):
    o = {"text": text, "to": to}
    if cond: o["if"] = cond
    if do: o["do"] = do
    return o
