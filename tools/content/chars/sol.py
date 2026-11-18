from dsl import D, O
d = D("sol")
d.entry("festival && !sol_epi && sol_dating && dating >= 3", "epi_all")
d.entry("festival && !sol_epi", "epi")
d.entry("sol_sailing", "rock")
d.entry("sol_dating && poly_open && !sol_poly_ack", "poly_ack")
d.entry("sol_date_set && !sol_date_done && at:arrival && time:evening", "date")
d.entry("has:brass_compass && has:sailcloth && has:mast_timber && sol_quest && !sol_quest_done", "parts_back")
d.entry("!met.sol", "first")
d.entry(None, "hub")

# ---------------------------------------------------------------- first meeting
d.n("first", "(A woman with a red bandana and sawdust in her curls vaults over the hull she's working on and lands right in front of you.) A real boat! From OUT THERE? No — don't tell me — let me guess — the world's still round? There are still oranges? Please say there are still oranges.",
    O("There are still oranges.", "first2"),
    O("Hello to you too.", "first2"))
d.n("first2", "(She punches the air.) I KNEW it. Marisol Reyes. Sol. Boatwright, rigger, and the finest sailor on an island nobody can sail away from. Which is hilarious, if you think about it. I try not to think about it.",
    O("What are you building?", "first3"))
d.n("first3", "The Gull's Wing. (She slaps the hull fondly.) Every boat that sails into that fog comes back round to the pier. So I'm building one that won't. Don't ask me how. I'm still working out the how.",
    O("I hope you make it.", "hub", do=["rel:sol:1"]))

# ---------------------------------------------------------------- hub
d.n("hub", "(Sol wipes her hands on a rag that makes them dirtier.) Hey, stranger. Need something rigged, mended, or flirted with?",
    O("Why do you want to leave so badly?", "boat", cond="!sol_boat_talk"),
    O("You keep looking at the horizon.", "quest", cond="rel.sol >= 2 && !sol_quest && !sol_quest_done"),
    O("About the parts for the Gull's Wing...", "quest_remind", cond="sol_quest && !sol_quest_done"),
    O("Can I help with the hull?", "time", cond="rel.sol >= 1 && !sol_time"),
    O("I found an old star chart.", "gift", cond="has:star_chart && !sol_gift"),
    O("I'd let you rig me any day.", "spark", cond="rel.sol >= 1 && !sol_spark && !sol_bestfriend"),
    O("About our sail...", "date_remind", cond="sol_date_set && !sol_date_done"),
    O("Hey, gorgeous.", "gf", cond="sol_dating"),
    O("What do you think of Wren and Ione?", "friends", cond="met.wren || met.ione"),
    O("The fog's gone, Sol.", "after", cond="lantern_lit && !sol_after"),
    O("See you, Sol.", "end"))
d.n("boat", "(For once she stops moving.) Because I was born on a rock with sixty people, and every single one of them has stopped wondering what's out there. I haven't. I can't. Some nights I stand at the end of the pier and I can feel the whole world on the other side of that grey, waiting.",
    O("And if there's nothing out there?", "boat2"),
    O("Now you know there is. I came from it.", "boat3"))
d.n("boat2", "(A beat.) Then I'll have looked. That's the whole point. You don't get to know unless you look. (She grins, and it's a little crooked.) Besides — you came from somewhere. So there's at least one thing out there, and it's cute.",
    O("Back to other things.", "hub", do=["set:sol_boat_talk", "rel:sol:1"]))
d.n("boat3", "(She looks at you like you just handed her the moon.) You did. You actually did. Do you know how long I've waited for someone to prove me right? (She laughs.) Don't answer that. Tobias says I'm insufferable when I'm right.",
    O("Back to other things.", "hub", do=["set:sol_boat_talk", "rel:sol:1"]))
d.n("friends", "Wren's my best friend. We've been getting into trouble together since we could walk. She pretends she hates people, but she cried at a goat's birthday once. Ione? Ione and I fight about tide tables. She's always right. It's infuriating. I'd die for her.",
    O("Back to other things.", "hub"))

# ---------------------------------------------------------------- small moments
d.n("time", "Ha! Grab that mallet. No, the other one. NO, the — perfect. (You spend the afternoon fitting planks while she sings filthy sea shanties and corrects your grip every five minutes. By sunset, a whole new strake is curved along the hull.)",
    O("We make a good team.", "time2"))
d.n("time2", "(She leans on the hull next to you, shoulder to shoulder, sweaty and pleased.) We do, don't we? You've got good hands. Slow, but good. (She bumps your hip.) Come back any time. I'll teach you the rest of the shanties. The really bad ones.",
    O("I'll hold you to that.", "hub", do=["set:sol_time", "rel:sol:1"]))
d.n("spark", "(She stares at you, then bursts out laughing so hard she has to hold onto the hull.) Oh, you did NOT. That was terrible. That was magnificent. (She points a chisel at you, grinning.) Careful, stranger. Keep that up and I'll start flirting back properly.",
    O("I'm counting on it.", "spark2"))
d.n("spark2", "(She steps very close, smelling of salt and pine resin, and tucks a curl of wood shaving behind your ear.) Then consider yourself warned. (She winks and goes back to work, humming.)",
    O("(Walk away slightly dazed.)", "end", do=["set:sol_spark", "rel:sol:1"]))
d.n("gift", "A star chart? A REAL one — with the sea lanes drawn in? (She unrolls it on the hull with shaking hands.) Look at this. Look! Routes to places I've never even heard of. Somebody sailed all of this. Where did you find it?",
    O("In a cave in the Keep's cliffs.", "gift2"))
d.n("gift2", "(She rolls it up, very carefully, and holds it against her chest like a cat.) This is going in the Gull's Wing's chart table. And when I sail her out of here, it'll be your chart that shows me the way. Thank you. Really. Really really.",
    O("It's yours.", "hub", do=["take:star_chart", "set:sol_gift", "rel:sol:1"]))

# ---------------------------------------------------------------- personal quest
d.n("quest", "(She sighs.) She's not finished. The Gull's Wing. I need three things I can't make on this rock, and I can't leave to get them, because — well. Fog. Ha.",
    O("Tell me what you need.", "quest2"))
d.n("quest2", "A brass ship's compass — a proper one; mine's a toy. Sailcloth — real canvas, not stitched-together blankets. And a mast spar of straight heartwood pine. There's no tree in the village straight enough.",
    O("Any idea where to look?", "quest3"))
d.n("quest3", "Old Tobias's boat, the Meridian, sank in the bay with a fine compass aboard. The Keep had stores of everything, sailcloth included, if anyone could get up there. And Wren swore she'd cut me a spar from the great fallen pine in the north-west wood. She probably did and forgot to tell me. She does that.",
    O("I'll get you all three.", "end", do=["set:sol_quest"]),
    O("I'll keep an eye out.", "end", do=["set:sol_quest"]))
d.n("quest_remind", "Brass compass — the Meridian, in the bay. Sailcloth — the Keep's stores. Heartwood spar — the fallen pine, north-west Mosswood. Wren might have left it waiting. (She ticks them off on tarry fingers.)",
    O("Got it.", "hub"))
d.n("parts_back", "(You set the compass, the canvas and the spar down on the boathouse floor. Sol stares at them, then at you, then at them.) You got them. All of them. (She picks up the compass and the needle swings, steady and true.) She's going to fly.",
    O("Let's finish her.", "parts2"))
d.n("parts2", "(Three days later, the Gull's Wing sits on the slipway with her mast stepped and her sail bent on. Sol walks around her twice without speaking.) She's done. She's actually done. And the fog's still out there. (She laughs, and it wobbles.) I don't even know if she'll beat it. But she's done.",
    O("She will. And so will you.", "parts3"))
d.n("parts3", "(She turns to you, eyes bright.) I couldn't have finished her without you. Nobody's ever believed in the stupid boat except me. (She wipes her eyes, leaving a tar smudge.) So. Stranger. What are we, you and me?",
    O("I want to be more than friends, Sol.", "romance", cond="sol_spark", do=["take:brass_compass", "take:sailcloth", "take:mast_timber", "set:sol_quest_done", "rel:sol:2"]),
    O("You're one of my best friends, Sol.", "friend", do=["take:brass_compass", "take:sailcloth", "take:mast_timber", "set:sol_quest_done", "rel:sol:2"]))
d.n("romance", "(She grins so wide it must hurt.) I knew it. I KNEW it. (She grabs your face and kisses your forehead, smack.) No. Not like this, covered in tar. Meet me at the end of the long pier at sunset. I'm taking you out on the water. Properly.",
    O("It's a date.", "end", do=["set:sol_romance", "set:sol_date_set"]),
    O("It's a date. (Wait until evening.)", "end", do=["set:sol_romance", "set:sol_date_set", "event:wait_evening:The afternoon slides toward a golden evening..."]))
d.n("friend", "(She whoops and hugs you so hard your feet leave the floor.) Best friends! Wren's going to be SO jealous. Actually she won't, she'll just make a face. I love her face. I love your face. I love everyone today!",
    O("(Laugh and hug her back.)", "end", do=["set:sol_bestfriend", "rel:sol:1"]))
d.n("date_remind", "End of the long pier, at sunset. Wear something you don't mind getting splashed. Bring nothing. I've packed everything. Including the good cider.",
    O("I'll meet you there this evening.", "hub"),
    O("Let's not wait. (Pass the time until evening.)", "end", do=["event:wait_evening:The afternoon slides toward a golden evening..."]))

# ---------------------------------------------------------------- the date
d.n("date", "(Sol is sitting on the end of the long pier with her feet dangling over the water, a little rowboat tied below.) There you are. Get in. I'm stealing you.",
    O("(Climb into the boat.)", "end", do=["event:sail_out"]),
    O("Give me a minute.", "end"))
d.n("rock", "(Gull Rock. The sea is gold and the sky is going pink, and the fog sits on the horizon like a sleeping animal. Sol has a blanket spread on the warm stone and two bottles of cider.) Isn't it stupid? The most beautiful place in the world and I can see the edge of it from here.",
    O("It's not stupid.", "rock2"))
d.n("rock2", "I come out here when I can't stand it. Just to look. (She passes you a bottle.) You know what I figured out? I've spent my whole life wanting to be somewhere else. And today I don't. Today I want to be exactly here. (She glances at you.) Guess why.",
    O("The cider?", "rock3"),
    O("Tell me.", "rock3"))
d.n("rock3", "(She laughs, then stops laughing.) You. Obviously you, you absolute barnacle. You came in out of the fog and made me want to stay put. That's the most terrifying thing anyone's ever done to me. (She takes your hand.) So — will you be mine?",
    O("Yes, Sol. I'm yours.", "yes", cond="dating == 0"),
    O("Before I answer — I'm already with someone. You deserve to know.", "poly", cond="dating >= 1"),
    O("I think we're better as friends.", "rock_friend"))
d.n("yes", "(She kisses you as the sun touches the water, salty and laughing and sure.) Good. GOOD. I'm going to tell absolutely everyone. I'm going to tell the gulls. (She lies back on the blanket and pulls you down beside her.) Let's watch the sun go. Then I'll row us home.",
    O("(Watch the sunset with her.)", "rock_end", do=["set:sol_dating", "set:sol_date_done", "rel:sol:2"]))
d.n("poly", "(She raises her eyebrows, then grins.) I know, you lovely idiot. The whole village knows. Tobias knows, and he doesn't know anything. (She squeezes your hand.) And you told me yourself, out here, instead of letting me find out. That's what matters.",
    O("You're really all right with it?", "poly2"))
d.n("poly2", "I grew up on a rock with sixty people. I've got enough love in me to sink a ship. Why would I want you to have less of it? (She grins.) As long as everybody's honest and nobody's hurt, I'm in. I'll talk to them myself. Probably loudly. Now ask me again.",
    O("Sol — knowing all of it — will you be mine?", "yes_poly"))
d.n("yes_poly", "(She kisses you as the sun touches the water.) Knowing all of it. Yes, yes, a hundred times. Now shut up and watch the sunset with me, then I'll row us home.",
    O("(Watch the sunset with her.)", "rock_end", do=["set:sol_dating", "set:sol_date_done", "set:sol_poly_ack", "set:poly_open", "rel:sol:2"]))
d.n("rock_friend", "(She's quiet for a moment, then clinks her bottle against yours.) Then you're my best friend, and you're stuck with me forever, and you have to come sailing with me when the fog lifts. That's the deal. No take-backs.",
    O("No take-backs.", "rock_end", do=["set:sol_bestfriend", "set:sol_date_done", "clear:sol_romance"]))
d.n("rock_end", "(The sun goes down in a blaze. The first stars come out. Sol rows you home through the dark, singing softly, while the harbour lamps of Gullhaven draw closer.)",
    O("(Return to the harbour.)", "end", do=["event:sail_back"]))
d.n("poly_ack", "(Sol throws an arm around your neck.) So! We had The Talk. Capital T. Ione made an agenda. AN AGENDA. Wren said 'fine' about fifty times. I cried, and I'll deny it. (Softer.) It was good. Really good. Nobody's hiding anything, and nobody's losing anything.",
    O("Thank you, Sol.", "poly_ack2"))
d.n("poly_ack2", "Don't thank me, just keep being honest with all of us. (She kisses your cheek, loudly.) And bring cider next time.",
    O("Deal.", "hub", do=["set:sol_poly_ack"]))

# ---------------------------------------------------------------- girlfriend + after
d.n("gf", "(She vaults the hull, kisses you, and vaults back before you can react.) Hello, handsome-beautiful-whatever-you-like. I'm working. Distract me for exactly one minute.",
    O("(Distract her for exactly one minute.)", "gf2"),
    O("I'll let you work.", "end"))
d.n("gf2", "(One minute becomes ten. Tobias walks past the boathouse door, sees you both, turns round and walks away again, muttering about kids these days.) ...Right. Back to work. (She doesn't move.)",
    O("(Back to the hub.)", "hub"))
d.n("after", "(Sol is on the end of the pier, staring at the empty, endless, sparkling sea.) It's gone. The fog. There's just — sea. All the way. I can see the mainland, I think. That smudge. Is that the mainland?",
    O("That's the mainland.", "after2"))
d.n("after2", "(She whoops so loudly the gulls scatter.) The Gull's Wing sails at the next fair wind. And you're coming. That's not a question. Oh — I'm so happy I could punch someone.",
    O("Please don't punch me.", "hub", do=["set:sol_after", "rel:sol:1"]))

# ---------------------------------------------------------------- epilogues
d.n("epi", "(Sol has a garland of sea-pinks in her curls and a cup in each hand.)",
    O("...", "epi_dating", cond="sol_dating"),
    O("...", "epi_friend", cond="!sol_dating && (sol_bestfriend || rel.sol >= 4)"),
    O("...", "epi_other", cond="!sol_dating && !sol_bestfriend && rel.sol < 4"))
d.n("epi_dating", "THERE you are. Here, drink this, then dance with me, then come sailing with me tomorrow and the day after and every day after that. (She kisses you, laughing.) The whole world's out there now. I want to see it with you.",
    O("(Dance with Sol.)", "end", do=["set:sol_epi", "rel:sol:1"]))
d.n("epi_friend", "Best friend! Here, take a cup. The Gull's Wing sails at dawn, and you're crew. Don't argue. I've already written your name on a hammock.",
    O("Aye aye, captain.", "end", do=["set:sol_epi"]))
d.n("epi_other", "Hey, stranger who isn't a stranger any more. You did it. You actually did it. When the Gull's Wing sails, come and wave us off, yeah?",
    O("I wouldn't miss it.", "end", do=["set:sol_epi"]))
d.n("epi_all", "(Wren, Ione and Sol are standing together by the well, and all three of them turn as you arrive. Sol is grinning, Ione is blushing, and Wren is trying very hard to look unimpressed.) Sol: 'There's our hero! We've had a meeting.'",
    O("...", "epi_all2"))
d.n("epi_all2", "Sol: 'We've decided you're not allowed to save any more islands without us.' Ione: 'It's in writing. I wrote it.' Wren: 'Shut up and come here.' (They pull you into the middle of them, and the lantern beam sweeps over all four of you.)",
    O("I love you. All of you.", "epi_all3"))
d.n("epi_all3", "(Sol throws her arms around all three of you at once.) Good, because you're stuck with us. Crew of four on the Gull's Wing. Now DANCE.",
    O("(Dance with them under the lantern.)", "end", do=["set:wren_epi", "set:ione_epi", "set:sol_epi", "set:all_three_scene"]))

d.bark(None, "Haul away, haul away...")
d.bark("!lantern_lit", "One day, fog. One day.")
d.bark("sol_quest_done", "She's done. She's really done!")
d.bark("sol_dating", "(Sol blows you a kiss.)")
d.bark("lantern_lit", "Fair winds tomorrow!")
