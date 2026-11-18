from dsl import D, O
d = D("wren")
d.entry("festival && !wren_epi && wren_dating && dating >= 3", "epi_all")
d.entry("festival && !wren_epi", "epi")
d.entry("wren_dating && poly_open && !wren_poly_ack", "poly_ack")
d.entry("wren_date_set && !wren_date_done && at:glade && time:night", "date")
d.entry("!met.wren", "first")
d.entry(None, "hub")

# ---------------------------------------------------------------- first meeting
d.n("first", "(A young woman in a green cloak drops out of nowhere onto the path, bow half-drawn, then lowers it.) You're the boat. The whole village is talking about you. Well — Tobias is talking about you, which is the same thing.",
    O("And you are?", "first2"),
    O("Do you always ambush strangers?", "first2b"))
d.n("first2b", "Only the interesting ones. You're the first stranger I've ever met, so the bar was low. (A crooked smile.) Wren Ashdown. I keep the Mosswood.",
    O("Keep it from what?", "first3"))
d.n("first2", "Wren Ashdown. Ranger. I keep the Mosswood.", O("Keep it from what?", "first3"))
d.n("first3", "Fire. Rot. Fools. Spiders. Mostly from forgetting itself — paths grow over if nobody walks them. My mum walked them every day of her life. Now I do. (She shrugs, a little too quickly.) What brings you under my trees?",
    O("I'm trying to reach Kestrel Keep.", "first_keep", cond="letter_identified"),
    O("Just exploring.", "first4"))
d.n("first_keep", "The Keep. (She whistles.) Nobody's walked that road in thirty years. The thorns ate it. Talk to me when you know what you're doing, and I'll tell you what I know.",
    O("Thanks, Wren.", "end", do=["rel:wren:1"]))
d.n("first4", "Then explore. Stay on the paths, don't eat the red mushrooms, and if you hear clicking in the deep wood, walk the other way. Fast. (She melts back between the trunks, then leans out again.) Nice to meet you, boat.",
    O("Nice to meet you too.", "end", do=["rel:wren:1"]))

# ---------------------------------------------------------------- hub
d.n("hub", "(Wren leans against a tree, picking a burr out of her braid.) Boat. What's on your mind?",
    O("Where are the Singing Stones?", "stones", cond="seek_song && !wren_guided"),
    O("I opened the old road.", "road", cond="puzzle1_solved && !wren_road"),
    O("You seem far away today.", "quest", cond="rel.wren >= 2 && !wren_quest && !wren_quest_done"),
    O("About your mother's bow...", "quest_remind", cond="wren_quest && !wren_quest_done && !has:mother_bow"),
    O("I found this in the Hollow Oak.", "bow_back", cond="has:mother_bow && !wren_quest_done"),
    O("Want to shoot a few arrows with me?", "time", cond="rel.wren >= 1 && !wren_time"),
    O("I found something you might like.", "gift", cond="has:owl_whistle && !wren_gift"),
    O("You're a lot nicer than you pretend to be.", "spark", cond="rel.wren >= 1 && !wren_spark && !wren_bestfriend"),
    O("Tonight, at the glade...", "date_remind", cond="wren_date_set && !wren_date_done"),
    O("Hey, you.", "gf", cond="wren_dating"),
    O("Tell me about the Mosswood.", "lore"),
    O("What do you think of Sol and Ione?", "friends", cond="met.sol || met.ione"),
    O("The fog's gone.", "after", cond="lantern_lit && !wren_after"),
    O("See you around, Wren.", "end"))
d.n("stones", "The Singing Stones? North on the old road, past the turning for the cairn. A ring of four, with the thorn wall behind them where the road climbs into the gorge. They're supposed to sing, if you know the song. They've never sung for me.",
    O("Hester taught me a lullaby.", "stones2", cond="knows_song"),
    O("I'll find the song.", "stones2b", cond="!knows_song"))
d.n("stones2", "Hester's lullaby? (She goes quiet.) Mum used to say the stones were carved by people who sang to the trees. Maybe they'll listen to you. Go and see. I'll be — around. Not following you. Obviously.",
    O("Obviously.", "end", do=["set:wren_guided", "rel:wren:1", "checkpoint:cp_forest"]))
d.n("stones2b", "Ione thinks it's a song? Then ask Hester. She knows every song on the island, and a few nobody else does. The stones are north, on the old road. You can't miss them, they're the big rocks looking smug.",
    O("Thanks, Wren.", "end", do=["set:wren_guided", "rel:wren:1", "checkpoint:cp_forest"]))
d.n("road", "I heard it from the lodge. The whole forest went quiet, and then the stones — sang. I didn't know they really could. (She looks at you sideways.) Mum would have liked you. Here. Arrows. You're walking into the Keep; don't walk in empty-handed.",
    O("Thank you, Wren.", "hub", do=["give:arrows", "set:wren_road", "rel:wren:1"]))
d.n("lore", "The Mosswood's older than the village. Older than the Keep. The deep wood to the south-west is all dark oaks and spiders; the north-west is spruce and stone. There's a hollow oak in the deep part bigger than the inn. And the falls — go and look at the falls. Look behind them, if you're brave.",
    O("Back to other things.", "hub"))
d.n("friends", "Sol's my best friend. She's a menace. We've been drinking each other under the table at the Gull & Lantern since we were old enough to lift a cup. Ione... (She considers.) Ione uses eleven words where one would do. But she remembers things for people. That's kind, in a way. Don't tell her I said that.",
    O("Back to other things.", "hub"))

# ---------------------------------------------------------------- small moments
d.n("time", "(She raises an eyebrow, then grins.) You're on. To the range. (Half an hour later, your arms ache and your arrows are everywhere except the target. Wren has split one of her own arrows with another.) Not bad. For a boat.",
    O("You're showing off.", "time2"),
    O("Teach me how you do that.", "time2"))
d.n("time2", "Breathe out, not in. Mum taught me that. (She adjusts your elbow, then steps back a little quickly.) There. Now you'll hit the hay, at least. That was — fun. Don't make it weird.",
    O("Wouldn't dream of it.", "hub", do=["set:wren_time", "rel:wren:1"]))
d.n("spark", "(She stares at you, and for a moment the ranger vanishes and there's just a startled woman with leaves in her hair.) ...Don't say things like that. I'll start believing them.",
    O("Maybe you should.", "spark2"),
    O("Sorry. Too much?", "spark3"))
d.n("spark2", "(She looks away, but she's smiling, and the tips of her ears are pink.) Go on, get out of my forest. Before I do something stupid like agree with you.",
    O("(Leave her smiling.)", "end", do=["set:wren_spark", "rel:wren:1"]))
d.n("spark3", "No. (A pause.) No, it was fine. It was nice, actually. People don't say nice things to me much. I'm prickly. Like a holly bush with opinions.",
    O("I like holly.", "end", do=["set:wren_spark", "rel:wren:1"]))
d.n("gift", "Is that — an owl whistle? (She turns it over in her hands.) Mum had one just like it. She used to call the owls down out of the trees with it. I lost hers when I was little. (She blows a soft note, and somewhere far off, an owl answers.)",
    O("It's yours.", "gift2"))
d.n("gift2", "(She doesn't say anything for a while. Then she hugs you, hard and quick, like an ambush.) Thank you. Don't tell Sol I hugged someone. She'll never let me live it down.",
    O("Your secret's safe.", "hub", do=["take:owl_whistle", "set:wren_gift", "rel:wren:1"]))

# ---------------------------------------------------------------- personal quest
d.n("quest", "(She's quiet for a long moment.) Mum's face is going. I can't hold it any more. I know she had freckles, and her hair was red like mine, but when I try to see her... it's like the fog got in. Everything near the Keep goes first, they say. She walked that road every day.",
    O("Is there anything that might help?", "quest2"))
d.n("quest2", "Her bow. She carved owls into it. If I could hold it, I think I'd see her again. It's in the Hollow Oak, in the deep wood. She hid it there the last winter, for me to find when I was ready. But the spiders moved in. And when I go near it, I'm fourteen again and she's...",
    O("I'll get it for you.", "quest3"),
    O("We'll figure something out.", "hub"))
d.n("quest3", "(She grips your arm.) Be careful. The deep wood is south-west, past the falls. Follow the trail until the trees go dark and the webs start. The bow's in a chest inside the trunk. Thank you, boat. I mean it.",
    O("I'll be back.", "end", do=["set:wren_quest"]))
d.n("quest_remind", "The Hollow Oak's south-west, past the falls, where the trail runs into the dark oaks. Spiders — big ones. Bring something sharp and a lot of nerve.",
    O("On my way.", "hub"))
d.n("bow_back", "(She sees it in your hands and all the colour leaves her face.) That's... (She takes it as if it might break. She runs her thumb over the carved owls, and suddenly she's laughing and crying at once.) I see her. Freckles and a crooked tooth and her stupid laugh. I can see her.",
    O("She sounds wonderful.", "bow_back2"))
d.n("bow_back2", "She was. (She wipes her face on her sleeve, furious about it.) You went into that nest of nightmares for me. Nobody's ever... Here. Take my bow. I've got hers now, and you'll need yours.",
    O("Thank you, Wren.", "bow_choice", do=["take:mother_bow", "give:wren_bow", "set:wren_quest_done", "set:wren_quest", "rel:wren:2"]))
d.n("bow_choice", "(She stands closer than she needs to, holding her mother's bow.) So. Boat. What are we, you and me?",
    O("I'd like to be more than friends, if you'd like that too.", "romance", cond="wren_spark"),
    O("You're one of the best friends I've got on this island.", "friend"))
d.n("romance", "(A slow, crooked smile.) I'd like that. I'd like that a lot. But I'm not doing this in a hurry, standing in the mud. Meet me in the firefly glade after dark. East of the lodge, with the pond. I'll show you something.",
    O("I'll be there.", "end", do=["set:wren_romance", "set:wren_date_set"]),
    O("I'll be there. (Wait for nightfall.)", "end", do=["set:wren_romance", "set:wren_date_set", "event:wait_night:Dusk creeps through the Mosswood..."]))
d.n("friend", "(She punches your shoulder, lightly.) Best friends, then. Good. I've only got one other, and she's a menace. You'll balance her out.",
    O("Deal.", "end", do=["set:wren_bestfriend", "rel:wren:1"]))
d.n("date_remind", "The firefly glade, after dark. East of the lodge, by the pond. Don't make me wait — I'll start talking to the owls, and they're terrible listeners.",
    O("I'll go once it's night.", "hub"),
    O("Let's not wait. (Pass the time until night.)", "end", do=["event:wait_night:Dusk creeps through the Mosswood..."]))

# ---------------------------------------------------------------- the date
d.n("date", "(Wren is sitting on the fallen log by the pond. All around her the dark is full of tiny drifting lights.) You came. Good. Sit. Watch.",
    O("(Sit beside her.)", "date2"))
d.n("date2", "(For a while neither of you says anything. The fireflies rise out of the grass in slow gold spirals.) Mum brought me here the night before she died. She said the fireflies are the forest remembering the sun. I didn't understand what she meant till now.",
    O("What did she mean?", "date3"))
d.n("date3", "That things don't really go, as long as something keeps a little bit of their light. (She bumps your shoulder with hers.) I've spent five years being angry at everyone. And then a boat washes up and goes and finds my mother's face for me. That's not fair, you know. I had a whole plan to be alone forever.",
    O("I'm sorry I ruined your plan.", "date4"),
    O("Plans change.", "date4"))
d.n("date4", "(She laughs, soft.) No you're not. (She turns to face you, and the fireflies light her freckles.) I like you. More than like. So. Ask me, before I lose my nerve and shoot something.",
    O("Wren Ashdown, will you be mine?", "yes", cond="dating == 0"),
    O("Before I ask — I'm already seeing someone. I want you to know that.", "poly", cond="dating >= 1"),
    O("I think we're better as friends.", "date_friend"))
d.n("yes", "(She kisses you, quick and fierce, then sits back looking astonished at herself.) Yes. Obviously yes. You're mine and I'm yours, and if anyone in the village says anything, I'll shoot their hat off. (She leans her head on your shoulder.) Stay a while. The fireflies aren't done.",
    O("(Stay with her.)", "end", do=["set:wren_dating", "set:wren_date_done", "rel:wren:2"]))
d.n("poly", "(She doesn't look away.) I know. It's a small island, and you're not exactly subtle. (A crooked smile.) I don't need to own anybody. I watched my mum love this whole forest and every person in it, and none of it made her love me less.",
    O("I don't want to hide anything from you.", "poly2"))
d.n("poly2", "Then don't. That's the only rule. Be honest with me, and with them, and I'll be honest with you. I'll talk to them myself — properly, like adults, over too much cider. (She takes your hand.) Now ask me again.",
    O("Wren Ashdown, will you be mine — knowing all of it?", "yes_poly"))
d.n("yes_poly", "Yes. Knowing all of it. (She kisses you, and laughs against your mouth.) The others had better be good to you, or they answer to me. Stay. The fireflies aren't done.",
    O("(Stay with her.)", "end", do=["set:wren_dating", "set:wren_date_done", "set:wren_poly_ack", "set:poly_open", "rel:wren:2"]))
d.n("date_friend", "(A long breath.) Okay. (She bumps your shoulder again.) Then you're my best friend, and you're stuck with me. That's not nothing, boat. That's not nothing at all.",
    O("It's a lot.", "end", do=["set:wren_bestfriend", "set:wren_date_done", "clear:wren_romance"]))
d.n("poly_ack", "(Wren catches your sleeve.) Hey. I talked to them. All of us, round a table at the Gull & Lantern, Hester pretending not to listen. It was awkward for about four minutes, and then Sol made a terrible joke, and it was fine.",
    O("And you're really all right with it?", "poly_ack2"))
d.n("poly_ack2", "I'm really all right with it. I don't have to be everything to you. I just have to be me, and honest. So do you. (She grins.) Besides, now I've got people to complain about you to.",
    O("Thank you, Wren.", "hub", do=["set:wren_poly_ack"]))

# ---------------------------------------------------------------- girlfriend + after
d.n("gf", "(She steals a kiss, glances around to make sure no one saw, and fails to look stern.) You're distracting me from very important ranger business. Like staring at moss.",
    O("I'll let you get back to the moss.", "end"),
    O("Walk with me a while?", "gf2"))
d.n("gf2", "(You walk the trails together until the light changes. She points out a badger sett, a woodpecker's hole, the tree where she broke her arm at nine. She holds your hand the whole way and pretends it's to stop you tripping.)",
    O("(Back to the path.)", "hub"))
d.n("after", "(Wren is looking up at the beam of light over the cliffs.) It's gone. The fog. I can see the whole sky. And I remember her — I don't even need the bow. I just remember.",
    O("I'm so glad.", "hub", do=["set:wren_after"]))

# ---------------------------------------------------------------- epilogues
d.n("epi", "(Wren has her mother's bow slung on her back, and the lanterns make her hair look like fire.)",
    O("...", "epi_dating", cond="wren_dating"),
    O("...", "epi_friend", cond="!wren_dating && (wren_bestfriend || rel.wren >= 4)"),
    O("...", "epi_other", cond="!wren_dating && !wren_bestfriend && rel.wren < 4"))
d.n("epi_dating", "There you are. (She threads her fingers through yours.) The first clear night in thirty years, and I'm spending it in a crowd. Look what you've done to me. (Softer.) I'm happy. That's what you've done. Dance with me before Sol steals you.",
    O("(Dance with Wren.)", "end", do=["set:wren_epi", "rel:wren:1"]))
d.n("epi_friend", "Boat! Over here. (She drags you into the circle by the well.) Best friends get first dance, it's a rule. I just made it up. Come on, before Corin falls over again.",
    O("(Dance with Wren.)", "end", do=["set:wren_epi"]))
d.n("epi_other", "Hey, boat. You did a good thing. The forest feels... lighter. Come walk the trails some time. I'll show you the good mushrooms.",
    O("I'd like that.", "end", do=["set:wren_epi"]))
d.n("epi_all", "(Wren, Ione and Sol are standing together by the well, and all three of them turn as you arrive. Sol is grinning, Ione is blushing, and Wren is trying very hard to look unimpressed.) Well. Look who finally showed up.",
    O("...", "epi_all2"))
d.n("epi_all2", "Sol: 'We've decided you're not allowed to save any more islands without us.' Ione: 'It's in writing. I wrote it.' Wren: 'Shut up and come here.' (They pull you into the middle of them, and the lantern beam sweeps over all four of you.)",
    O("I love you. All of you.", "epi_all3"))
d.n("epi_all3", "(Wren presses her forehead to yours.) We know, boat. We talked about it. A lot. Turns out we love you too. Now dance with us, before the music stops.",
    O("(Dance with them under the lantern.)", "end", do=["set:wren_epi", "set:ione_epi", "set:sol_epi", "set:all_three_scene"]))

d.bark("!met.wren", "Hm.")
d.bark("met.wren && !wren_dating", "Stay on the paths, boat.")
d.bark(None, "Woodpecker. Left. No — your other left.")
d.bark("wren_dating", "(Wren winks at you.)")
d.bark("wren_quest_done", "I can see her face again.")
d.bark("lantern_lit", "Look at all those stars.")
