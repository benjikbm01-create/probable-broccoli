from dsl import D, O
d = D("corin")
d.entry("festival && !corin_epi", "fest")
d.entry("has:keep_steel && corin_asked && !side_corin_done", "steel_back")
d.entry("!met.corin", "first")
d.entry(None, "hub")

d.n("first", "(A broad man with a soot-black beard looks up from the anvil, hammer still raised.) Hah! The famous stranger. Corin Ashby, smith, farrier, and the only man on Ysmere who can fix a kettle. Mind the sparks, they've no manners.",
    O("Nice to meet you, Corin.", "first2"))
d.n("first2", "You came in through the fog, they say. Shame you didn't bring any iron with you. Or coal. Or news. Ah, I'm only teasing. Welcome to Gullhaven. If anything of yours breaks, it's mine to mend.",
    O("I'll keep it in mind.", "hub", do=["rel:corin:1"]))

d.n("hub", "What can I do for you?",
    O("I'm heading out of the village.", "sword", cond="letter_identified && !corin_sword"),
    O("Your forge seems cold.", "quest", cond="met.corin && letter_identified && !corin_asked"),
    O("About that Kestrel steel...", "quest_remind", cond="corin_asked && !side_corin_done && !has:keep_steel"),
    O("I saw a cat called Biscuit by the inn.", "biscuit", cond="!corin_biscuit"),
    O("What do you make of Wren?", "wren", cond="met.wren"),
    O("See you, Corin.", "end"))
d.n("sword", "Out where? (He follows your gaze toward the Mosswood, then north.) The Keep. I knew it. Nobody asks about roads unless they mean to walk them. Here. It's plain, but it's honest, and it'll hold an edge.",
    O("Thank you!", "hub", do=["give:smith_sword", "set:corin_sword"]))
d.n("quest", "Cold? Friend, this forge hasn't been properly hot in thirty years. The good steel all came from the Keep. Kestrel steel, stamped with the bird. Stockpiles of it in the armoury up there, my old master said.",
    O("And if I brought you some?", "quest2"))
d.n("quest2", "(His eyes light up like coals catching.) Three ingots. Just three, and I could make you something worth carrying. Something with a story in it. Will you?",
    O("I'll look for it in the Keep's armoury.", "end", do=["set:corin_asked"]),
    O("Not right now.", "hub"))
d.n("quest_remind", "The armoury, up in the Keep. East side, if my master's stories were true. Three ingots stamped with a kestrel. Mind yourself — old places don't like visitors.",
    O("Right.", "hub"))
d.n("steel_back", "Is that — (He takes the ingots as if they were eggs.) Kestrel steel. Look at the grain on it. They don't make it like this any more. They don't make it at all. Give me the night.",
    O("Take your time.", "steel_back2"))
d.n("steel_back2", "(The forge roars properly for the first time in years. Hours pass, and the whole village comes to watch the sparks. At last he lays a blade across your hands.) The Kestrel Blade. It'll cut true and it won't dull. Carry it somewhere worth going.",
    O("It's magnificent. Thank you.", "end", do=["take:keep_steel:3", "give:kestrel_blade", "set:side_corin_done", "rel:corin:2"]))
d.n("biscuit", "Biscuit! (He drops the hammer.) Grey, fat, judgemental? That's my cat! Been gone a week — trust her to move into the inn where the cooking's better. Ha! Thank you. I'll go and fail to bring her home.",
    O("Good luck.", "hub", do=["set:corin_biscuit", "rel:corin:1"]))
d.n("wren", "Wren? Comes in every other day for arrowheads and tells me they're too heavy, too light, too pointy. I've known her since she was a sprout. She's lonelier than she lets on. Her mother was the ranger before her. Fine woman. Gone five years.",
    O("Back to other things.", "hub"))
d.n("fest", "There you are! Grab a cup. No — two cups. You've earned the both of them. Clear skies over Ysmere and the forge burning hot. Life's good, friend. Life's good.",
    O("It really is.", "end", do=["set:corin_epi"]))

d.bark(None, "(clang) (clang) (clang)")
d.bark("!side_corin_done", "Cold forge, cold heart...")
d.bark("side_corin_done", "Kestrel steel! Ha!")
d.bark("corin_biscuit", "Biscuit, come home, you traitor.")
