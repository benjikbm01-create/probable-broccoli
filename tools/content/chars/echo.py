from dsl import D, O
d = D("echo")
S = "A memory in the water"
d.entry("at:echo_1 && !echo_1", "e1")
d.entry("at:echo_2 && !echo_2", "e2")
d.entry("at:echo_3 && !echo_3", "e3")
d.entry(None, "quiet")

d.n("quiet", "The pool is still. Whatever it held has already been remembered.", O("(Step back.)", "end"), speaker=S)

d.n("e1", "(The water shivers. Wind and rain roar out of nowhere. You see a harbour wall in a storm, and a woman holding a small girl's hand.) 'Hold on to me, Elsie! Hold on —' (A wave rises over the wall, black and enormous.)",
    O("(Keep watching.)", "e1b"), speaker=S)
d.n("e1b", "(The wave falls. When it draws back, the wall is empty. Far above, on the cliff, the great lantern burns gold. A man is running down the cliff road, shouting two names into the wind: 'Annis! ELSIE!')",
    O("(The vision fades.)", "after", do=["set:echo_1"]), speaker=S)

d.n("e2", "(The water glows. You see a study in the Keep, grey with dawn. The man from the storm sits with a burning stone cupped in his hands. His face is ruined with grief.) 'What the sea takes, the Wellspring keeps. That's what the walls say. Then I'll go down and take them back.'",
    O("(Keep watching.)", "e2b"), speaker=S)
d.n("e2b", "(He climbs the tower, lifts the Ember from its cradle, and the great lantern goes dark behind him. Out at sea, a grey wall of fog begins to rise. He doesn't look back.) 'One night. The lantern can wait one night.'",
    O("(The vision fades.)", "after", do=["set:echo_2"]), speaker=S)

d.n("e3", "(The water is pale and calm. A shingle beach at dawn, after the storm. An old woman in a shawl — you think of Wendel's Maud — kneels beside a small, soaked girl curled among the stones. Around the girl's neck: a tiny silver gull.)",
    O("(Keep watching.)", "e3b"), speaker=S)
d.n("e3b", "'Hush, little one. Hush. What's your name, love?' (The girl only hums — a lullaby you know.) 'Never mind. Let's get you warm. The Mosses at the inn will know what to do.' (She wraps the girl in her shawl and carries her up toward Gullhaven.)",
    O("(The vision fades. You know who that girl grew up to be.)", "after", do=["set:echo_3", "set:knows_hester"]), speaker=S)

d.n("after", "(The pool goes still.)",
    O("(Step back.)", "end", cond="!(echo_1 && echo_2 && echo_3)"),
    O("(Step back.)", "end", cond="echo_1 && echo_2 && echo_3", do=["event:open:echo_door", "toast:The Hall of Echoes|Somewhere to the south, iron grinds open."]),
    speaker=S)
