from dsl import D, O
d = D("wendel")
d.entry("festival && !wendel_epi", "fest")
d.entry("lantern_lit && !wendel_remembers", "remember")
d.entry("waystones_lit >= 4 && wendel_asked && !side_wendel_done", "lamps_done")
d.entry("!met.wendel", "first")
d.entry(None, "hub")

d.n("first", "Eh? Speak up, the sea's in my ears. (An old man squints at you from under white eyebrows.) A new face. I'd say I'd remember it, but I'd be lying. I'm Wendel. Wendel Pike. I fish, and I forget.",
    O("I'm new here. I came in on a boat.", "first2"))
d.n("first2", "A boat! Ha. Maud would have liked you. She liked anything that came in from the sea — driftwood, glass floats, lost things. (His eyes go far away.) She found the strangest things on that shore.",
    O("Who's Maud?", "first3"))
d.n("first3", "My wife. Gone eleven years now. I'm buried next to her already, in my head; the rest of me's just slow to follow. Ah, don't look like that. Sit a while, if you like. Old men know things, now and then. Ask me if you're stuck.",
    O("I'll remember that.", "hub", do=["rel:wendel:1"]))

d.n("hub", "Well, now. What is it?",
    O("I'm stuck. Any advice?", "advice"),
    O("Tell me about Maud.", "maud"),
    O("Do you need help with anything?", "lamps", cond="!wendel_asked"),
    O("About the waystone lamps...", "lamps_remind", cond="wendel_asked && !side_wendel_done"),
    O("Ione's looking for a page of the Chronicle.", "page", cond="ione_quest && !got.page_1 && !ione_quest_done"),
    O("What do you remember about the great storm?", "storm"),
    O("Goodbye, Wendel.", "end"))
d.n("advice", "Hm. Let me think on it... (He chews his pipe stem for an age.) There. I've a notion. Mind you don't tell anyone it came from me; they'll want more.",
    O("Thank you.", "end", do=["event:hint"]))
d.n("maud", "Maud kept the waystone lamps on the island paths lit, every night, so folk could find their way home in the fog. Walked every mile of this island in all weathers. Said lost things always find their way home if you leave a light for them.",
    O("She sounds wonderful.", "hub", do=["rel:wendel:1"]))
d.n("storm", "The great storm. Thirty-one winters back. (He frowns, groping.) There was a — a wave, at the harbour. People lost. Maud went down to the shingle at dawn, the way she always did after a blow. She came home soaked to the knee and wouldn't say a word about it for a day. And then — then it's gone. Like smoke.",
    O("Back to other things.", "hub"))
d.n("lamps", "Since you ask. Maud's waystone lamps. Four of them, on the old paths: one on the harbour headland, one by the falls in the Mosswood, one on the Cairn Hill path, and one at the top of the old road by the Keep. They've been dark since she passed. My knees don't do hills.",
    O("I'll light them for you.", "lamps2"),
    O("Maybe later.", "hub"))
d.n("lamps2", "You would? Here — her tinderbox. Just strike it at each lamp. (He presses a battered little box into your hand and holds on a moment longer than he needs to.) Lost things find their way home. She'd say so.",
    O("I'll bring it back when they're lit.", "end", do=["set:wendel_asked", "give:tinderbox"]))
d.n("lamps_remind", "Four lamps: the harbour headland, the falls, the Cairn Hill path, and the top of the old road by the Keep. The top one you'll only reach once that thorn mess is cleared, I'd wager.",
    O("I'll keep at it.", "hub"))
d.n("lamps_done", "(Wendel is looking toward the headland, where a small flame burns.) I saw it last night. Her lamp, lit again. I sat out on the bench and cried like a boy. (He clears his throat fiercely.) Right. Enough of that.",
    O("She'd be proud of you.", "lamps_done2"))
d.n("lamps_done2", "Here. Two things. Her charm — she carved it from driftwood; it's kept me alive through three winters I'd no business surviving. And her diary. I can't read it any more. My eyes, and... other reasons. Maybe it'll mean something to you.",
    O("Thank you, Wendel.", "end", do=["take:tinderbox", "give:wendel_charm", "give:maud_diary", "set:side_wendel_done", "rel:wendel:2"]))
d.n("page", "A page? (He rummages in a tin of fish hooks and comes up with a folded, yellow sheet.) Ione's Chronicle, eh. Maud borrowed it to copy out a recipe and never gave it back. Forty years overdue. Tell Ione I'm sorry, and to take it up with Maud.",
    O("I'll pass that on.", "hub", do=["give:page_1"]))
d.n("remember", "(Wendel grips your arm.) I remember the morning. Maud came home soaked with a little girl wrapped in her shawl. Half-drowned, wearing a silver gull. The Warden's girl. Elsie Vael. She took her to the Mosses at the inn because we'd no idea where her people were — and then we forgot. All of us. Thirty years, she's been pouring my beer.",
    O("She's going to be all right.", "remember2"))
d.n("remember2", "Aye. She always was the strong one. Like Maud. Go on — there'll be dancing tonight, and I intend to fall over at least twice.",
    O("Save me a dance.", "end", do=["set:wendel_remembers"]))
d.n("fest", "Look at them all. Thirty years of grey and now this. Maud, you should see it. (He raises his cup to the sky.) Here's to lost things finding their way home.",
    O("To lost things.", "end", do=["set:wendel_epi"]))

d.bark(None, "Hm? No, no, just thinking.")
d.bark("!lantern_lit", "Fog's thick today. Always is.")
d.bark("side_wendel_done", "Her lamp's burning. Heh.")
d.bark("lantern_lit", "I can see the mainland! Maud!")
