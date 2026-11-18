from dsl import D, O
d = D("hester")
d.entry("festival && !hester_epi", "epi")
d.entry("festival", "epi_after")
d.entry("lantern_lit", "lantern")
d.entry("!met.hester", "first")
d.entry(None, "hub")

d.n("first", "Mind the step, it bites — oh! You're not from here. Nobody's not from here. Sit, sit before you fall down. You look like the sea chewed you and spat you out.",
    O("Tobias said you might have a room.", "first2"),
    O("Is it that obvious?", "first2"))
d.n("first2", "Hester Moss. This is the Gull & Lantern, and you're the first new face through that door in my whole life. Of course you'll have a room. The one at the top of the stairs, on the left, with the blue quilt. It's yours as long as you like.",
    O("I can't pay you much.", "first3"),
    O("Thank you. Really.", "first3"))
d.n("first3", "Pay me in news of the world, then. Here — bread, and take more whenever you need it. Now. Tobias doesn't send people to me for the bread. What's troubling you?",
    O("I received a letter. It brought me here.", "letter", do=["checkpoint:cp_inn", "give:bread", "set:room_given"]))
d.n("letter", "(She reads it twice, and goes very still.) 'Ysmere is fading from the world, and it is my fault.' Who on earth would write that? 'E.V.'... (A small, puzzled laugh.) My own initials are wrong by both letters. No, I don't know anyone it could be.",
    O("And the seal?", "letter2"))
d.n("letter2", "A kestrel. Pretty thing. Ione at the Archive would know it — she has a whole drawer of old seals and she labels the labels. She takes her tea here every afternoon, but you'll find her at the Archive most of the day.",
    O("I'll go and see her.", "hub"))

d.n("hub", "What can I get you, love?",
    O("Could I have something to eat?", "food"),
    O("I'd like to rest a while.", "rest"),
    O("Could you patch me up?", "heal"),
    O("Ione says you might know an old song.", "song", cond="seek_song && !knows_song"),
    O("Would you sing the old song again?", "song_again", cond="knows_song"),
    O("Tobias says you brew a Diver's Draught.", "draught", cond="tobias_asked && !side_tobias_done && !has:diver_draught"),
    O("Tell me about yourself.", "self"),
    O("Hester... do you own a silver locket?", "locket", cond="echo_3 && !hester_locket_talk"),
    O("How are you holding up?", "worry", cond="saw_empty_lantern"),
    O("I'll be off.", "end"))
d.n("food", "Here — still warm. No arguments.", O("Thank you!", "hub", do=["event:food"]))
d.n("heal", "Sit. Let me see. (She dabs something stinging on your scrapes and presses a cup into your hands.) There. You'll live, more's the pity for the gulls.",
    O("Thanks, Hester.", "hub", do=["event:heal"]))
d.n("rest", "Your room's up the stairs. Or I can just let you doze by the fire. How long?",
    O("Until evening.", "end", do=["event:wait_evening:You doze by the fire while the inn murmurs around you..."]),
    O("Until nightfall.", "end", do=["event:wait_night:The fire burns low. Outside, night settles over Gullhaven..."]),
    O("Until morning.", "end", do=["event:rest:You sleep soundly under the blue quilt."]),
    O("Never mind.", "hub"))
d.n("song", "The old song? You mean the lullaby. (She laughs, embarrassed.) It's silly. I hum it when I'm kneading. Mother Moss used to say I came to her already humming it, before I could even say my name.",
    O("Would you sing it for me?", "song2"))
d.n("song2", "(She closes her eyes and sings, softly, and the whole tavern seems to lean in to listen.)\n'Heron wakes the silver tide, / Stag walks where the shadows hide, / Owl keeps watch from the starlit tree, / Hare runs home, home to me.'",
    O("That's beautiful. Where does it come from?", "song3"))
d.n("song3", "I've no idea. No one else on the island knows it. I wrote it out once, for the children — the book's on the lectern by the door, if you want it. Why does Ione care about a lullaby?",
    O("She thinks it might open the old road.", "song4"))
d.n("song4", "A lullaby, opening roads? (She smiles, uncertain.) Well. Stranger things have washed in from the fog today. Take care in the Mosswood, love. Find Wren if you get lost — she'll pretend she's not glad to help.",
    O("I will.", "hub", do=["set:knows_song", "set:stones_awake", "give:lullaby", "toast:The lullaby|Heron, stag, owl, hare."]))
d.n("song_again", "'Heron wakes the silver tide, / Stag walks where the shadows hide, / Owl keeps watch from the starlit tree, / Hare runs home, home to me.' (She hums the last line again, half to herself.)",
    O("Thank you.", "hub"))
d.n("draught", "For the Meridian? (She raises an eyebrow.) Tobias never asks anyone for anything. Here. Drink it just before you dive — you'll breathe like a seal for a good while. Bring the old grump something good back.",
    O("I'll do my best.", "hub", do=["give:diver_draught"]))
d.n("self", "Me? I was raised here by the Mosses, who had the inn before me. They took me in when I was small. Lost at sea, my real people, or so I was told. I don't remember them at all. I used to mind. Now I've got a whole village to look after instead.",
    O("You've never wanted to leave?", "self2"),
    O("Back to other things.", "hub"))
d.n("self2", "Leave? Where would I go, the fog? (She looks out of the window.) Sometimes I dream of a tall tower with a light at the top, and a man singing. Then I wake up and the bread's burning. Life on Ysmere.",
    O("Back to other things.", "hub"))
d.n("worry", "Better for seeing you walk in on two feet. The whole village is whispering about you going up to that Keep. I keep setting an extra place at supper without meaning to. Silly.",
    O("Not silly at all.", "hub", do=["rel:hester:1"]))
d.n("locket", "(Her hand goes to her collar, where a chain glints.) How did you know? I've had it as long as I can remember. There's a gull on the front. It's never opened; the clasp is rusted. Why do you ask?",
    O("I think I know where you came from.", "locket2"),
    O("Just a feeling. Never mind.", "hub", do=["set:hester_locket_talk"]))
d.n("locket2", "(For a moment her face is a child's face, frightened and hopeful.) Don't. Please. Not until you're sure. I've wondered my whole life, and I couldn't bear it to be wrong.",
    O("When I'm sure, I'll tell you.", "hub", do=["set:hester_locket_talk", "set:hester_hinted", "rel:hester:1"]))
d.n("lantern", "(Hester is sitting on the inn steps, the light of the great lantern on her face. She's been crying.) Something's happening to me. Pieces. A wave. A hand slipping out of mine. A song. I don't understand.",
    O("I'll explain everything. Tonight.", "end"))
d.n("epi", "(Hester stands apart from the crowd, holding her locket — open now, a tiny painted face inside.) I remember. All of it. The storm, and the harbour wall, and my mother's hand. And my father, on the tower stairs, singing me that song.",
    O("Your father was the Warden.", "epi2"))
d.n("epi2", "Edric Vael. And I was Elspeth. Elsie. E.V. (She laughs and it breaks in the middle.) The letter. He wrote it, didn't he. Down there in the dark, all those years.",
    O("He did. I told him you'd lived.", "epi3_told", cond="told_warden"),
    O("He did. He's at peace now.", "epi3"))
d.n("epi3_told", "You told him. (She presses the locket to her lips.) Then the last thing he knew was that I was alive. Thank you. Thank you for giving him that.",
    O("He was so proud of you.", "epi4"))
d.n("epi3", "At peace. (She nods slowly.) I'll go up to the Keep when I'm ready, and sing for him. It's his song. He should have it back.",
    O("I'll go with you, if you like.", "epi4"))
d.n("epi4", "I'll still be Hester, you know. Hester Moss of the Gull & Lantern. I just get to be Elsie too, now. Go on — Tobias is lighting the harbour lamps at the end of the pier. Find anyone you want beside you, and come and watch.",
    O("Walk with me to the pier now.", "end", do=["set:hester_epi", "event:credits"]),
    O("I'll see a few people first.", "end", do=["set:hester_epi"]))
d.n("epi_after", "The pier's waiting whenever you are, love. The lamps won't light themselves. Well — Tobias will light them. Grumbling.",
    O("Let's go to the pier.", "end", cond="!credits_seen", do=["event:credits"]),
    O("Something to eat first?", "food"),
    O("In a while.", "end"))

d.bark("!lantern_lit", "Bread's in, kettle's on.")
d.bark("!lantern_lit", "Heron wakes the silver tide...")
d.bark("knows_song", "Hare runs home, home to me...")
d.bark("met.hester", "Mind you eat something!")
d.bark("lantern_lit", "Papa... no. Hester, the soup.")
