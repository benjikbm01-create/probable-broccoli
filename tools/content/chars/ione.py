from dsl import D, O
d = D("ione")
d.entry("festival && !ione_epi && ione_dating && dating >= 3", "epi_all")
d.entry("festival && !ione_epi", "epi")
d.entry("ione_dating && poly_open && !ione_poly_ack", "poly_ack")
d.entry("ione_date_set && !ione_date_done && at:archive_roof && time:night", "date")
d.entry("has:page_1 && has:page_2 && has:page_3 && ione_quest && !ione_quest_done", "pages_back")
d.entry("!met.ione", "first")
d.entry(None, "hub")

# ---------------------------------------------------------------- first meeting & the seal
d.n("first", "(A woman with ink on her fingers and a pencil through her bun looks up from a towering stack of books.) Oh! You're — you're the person from the boat. You're real. Sorry. That's a strange thing to say. I'm Ione. Ione Calder. I keep the Archive. I keep everything, really, it's a sort of condition.",
    O("Hester said you might be able to read a seal.", "seal", cond="!letter_identified"),
    O("Nice to meet you, Ione.", "first2"))
d.n("first2", "Nice to meet you too. I'm writing that down. (She actually writes it down.) I write everything down. It's the only way anything stays, on Ysmere. Was there something you needed?",
    O("I have a letter with a strange seal.", "seal", cond="!letter_identified"),
    O("Just saying hello.", "hub", do=["rel:ione:1"]))
d.n("seal", "A seal? Let me — oh. (She goes very pale.) That's a kestrel. That's the seal of the Wardens of Kestrel Keep. It can't be. Nothing has come out of the Keep in thirty years. Nobody even remembers who the last Warden was.",
    O("Why doesn't anyone remember?", "seal2"))
d.n("seal2", "Come and look. (She pulls down a huge ledger and opens it. Half of every page is blank, as if the ink simply let go.) Every record about the Keep fades. I copy them out; the copies fade too. Everything near the Keep goes first — names, dates, faces. Whatever is happening to Ysmere, it started up there.",
    O("Then I need to go to the Keep.", "seal3"))
d.n("seal3", "(She flips frantically through another book.) Here — one line that stayed. 'The road to the Keep runs where the stones sing, and the stones sing only to the old song.' The old road through the Mosswood. It's been choked with thorns for decades.",
    O("What old song?", "seal4"))
d.n("seal4", "I don't know! Which is maddening. But if anyone on this island knows an old song, it's Hester — she hums things nobody else has ever heard. And Wren, the ranger, knows every stone in that forest. Ask them both. And — please come back and tell me everything. I'll write it down.",
    O("I'll come back. I promise.", "end", do=["set:letter_identified", "set:seek_song", "rel:ione:1"]))

# ---------------------------------------------------------------- hub
d.n("hub", "(Ione looks up from her writing and smiles, distractedly, then properly.) Hello again. What have you found? Tell me everything. Slowly. My hand cramps.",
    O("I read the Warden's log in the Keep.", "log", cond="found_log && !ione_log"),
    O("The great lantern is empty.", "lantern", cond="saw_empty_lantern && !ione_lantern"),
    O("I saw the murals beneath the Keep.", "murals", cond="echo_1 && !ione_murals"),
    O("You look worried.", "quest", cond="rel.ione >= 2 && !ione_quest && !ione_quest_done"),
    O("About the missing pages...", "quest_remind", cond="ione_quest && !ione_quest_done"),
    O("Read with me a while?", "time", cond="rel.ione >= 1 && !ione_time"),
    O("I brought you something.", "gift", cond="has:sea_lily && !ione_gift"),
    O("You're adorable when you're flustered.", "spark", cond="rel.ione >= 1 && !ione_spark && !ione_bestfriend"),
    O("About tonight, on the roof...", "date_remind", cond="ione_date_set && !ione_date_done"),
    O("Hello, you.", "gf", cond="ione_dating"),
    O("What's your Book of Days?", "book"),
    O("What do you think of Wren and Sol?", "friends", cond="met.wren || met.sol"),
    O("The fog has lifted.", "after", cond="lantern_lit && !ione_after"),
    O("Where should I go next?", "next", cond="letter_identified && !lantern_lit"),
    O("I'll let you work.", "end"))
d.n("book", "(She holds up a battered notebook, thick as a brick.) My Book of Days. Every night I write down what happened, and every morning I read it, before anything else. People here forget. Their own birthdays. Their friends. I decided, when I was nineteen, that I wasn't going to let it take me.",
    O("Does it work?", "book2"))
d.n("book2", "Mostly. (She hesitates.) Sometimes I read things I wrote and they feel like someone else's life. But I'd rather have someone else's life than none. Is that strange?",
    O("It's brave.", "hub", do=["rel:ione:1"]),
    O("Not strange at all.", "hub"))
d.n("friends", "Wren thinks I talk too much. She's correct, but she doesn't have to say it with her eyebrows. She's kind, underneath. Sol is... (She sighs.) Sol borrowed the harbour records to 'check a tide' four years ago and returned them with a drawing of a whale on the cover. A good whale. Still.",
    O("Back to other things.", "hub"))
d.n("next", "(She consults three different notebooks.) If you're lost, Wendel Pike is surprisingly wise and very unsurprisingly slow. Or look in your journal; you'll have written more than you think. And if all else fails, come back here and I'll make you a list. I love making lists.",
    O("Back to other things.", "hub"))
d.n("log", "(She listens, her pencil frozen.) He took the Ember. Below. To bring his family back from the — from the sea. (She sits down.) Grief made him break the one rule the Lanternwrights wrote on every wall. That's the most human thing I've ever heard, and the most terrible.",
    O("Do you know his name now?", "log2"))
d.n("log2", "No. It's on the page and it slides off my eyes. Edwin? Edgar? (She writes, and the ink seems to pale as she watches.) Find out what happened to him. Please. Someone should remember him properly.",
    O("I will.", "hub", do=["set:ione_log", "rel:ione:1"]))
d.n("lantern", "Empty. Then the Ember is down there — wherever 'below' is. (She tears through a book of old plans.) There! The Keep was built over older halls. The Lanternwrights' halls. The way down would be the cistern, in the courtyard. It'll be locked. Wardens kept keys for everything.",
    O("I found the Warden's Key.", "hub", cond="has:warden_key", do=["set:ione_lantern", "rel:ione:1"]),
    O("Then I'll find a key.", "hub", do=["set:ione_lantern", "rel:ione:1"]))
d.n("murals", "You saw them. Real Lanternwright murals. (She grabs your hands, then lets go, embarrassed.) Sorry. Tell me exactly what they said. (You tell her. She writes furiously.) 'What the sea takes, the Wellspring keeps.' And 'Never carry the flame below.' Oh, the poor man.",
    O("Back to other things.", "hub", do=["set:ione_murals", "rel:ione:1"]))

# ---------------------------------------------------------------- small moments
d.n("time", "(Her face lights up.) Really? Most people's eyes glaze over by the second footnote. (You sit together by the window while she reads aloud from the Chronicle — sea monsters, a goat that became mayor, a feud over a lighthouse. She does all the voices.)",
    O("You're a wonderful storyteller.", "time2"))
d.n("time2", "(She blinks, and goes pink.) I am? Nobody's ever... I usually just read to the cat. The Archive cat. She's very critical. Thank you. That was the nicest afternoon I've had in — I'll check, but probably ever.",
    O("We should do it again.", "hub", do=["set:ione_time", "rel:ione:1"]))
d.n("spark", "I'm not — I'm not flustered, I'm — (She drops her pencil. Picks it up. Drops it again.) This is a measured scholarly response to an unexpected stimulus.",
    O("Of course it is.", "spark2"))
d.n("spark2", "(She covers her face with a ledger. From behind it, muffled:) I'm writing this down later. In detail. Go away before I say something I'll have to cross out.",
    O("(Leave her blushing behind the ledger.)", "end", do=["set:ione_spark", "rel:ione:1"]))
d.n("gift", "Is that a sea-lily? From Gull Rock? (She takes it as if it were spun glass.) They only grow on the very top of the stack. I've read about them since I was a girl. Nobody climbs up there. You climbed up there? For me?",
    O("For you.", "gift2"))
d.n("gift2", "(She opens her Book of Days and presses the flower carefully between the pages of today.) There. Now this day can't fade. Even if the ink does, the flower won't. Thank you. Truly.",
    O("You're welcome.", "hub", do=["take:sea_lily", "set:ione_gift", "rel:ione:1"]))

# ---------------------------------------------------------------- personal quest
d.n("quest", "(She closes a book very gently.) Three pages are missing from the Chronicle. The oldest part — the founding of the village. I've searched for years. And the strangest thing: in my Book of Days, eleven years ago, I wrote 'Found the third page. Must not lose it.' And then nothing.",
    O("Where should I look?", "quest2"))
d.n("quest2", "Anywhere and everywhere, which is useless, I know. But — Wendel's wife borrowed half this library once. The Keep had its own copy of everything. And Pell's shop kept ledgers wrapped in old paper. If you happen to see a page in faded brown ink, with a lantern drawn in the corner...",
    O("I'll find all three.", "end", do=["set:ione_quest"]),
    O("I'll keep an eye out.", "end", do=["set:ione_quest"]))
d.n("quest_remind", "Three pages, faded brown ink, a little lantern drawn in each corner. Wendel's cottage, the Keep, and Pell's shop are my best guesses. (She ticks them off on inky fingers.)",
    O("Got it.", "hub"))
d.n("pages_back", "(You lay the three pages on her desk. She doesn't breathe.) The founding pages. All three. (She reads the first two quickly, hungrily. Then she reads the third, and her hand starts to shake.)",
    O("What is it?", "pages2"))
d.n("pages2", "This is my handwriting. Eleven years ago. I copied the third page because the original was crumbling, and I wrote a note in the margin: 'If you are reading this, Ione, you forgot. That's all right. Keep going.' (Her voice cracks.) I don't remember writing this. I don't remember any of it.",
    O("You kept going. That's what matters.", "pages3"),
    O("Hey. You're still here. You're still you.", "pages3"))
d.n("pages3", "(She laughs wetly.) I was so scared of forgetting myself that I never noticed I'd already left myself a note. (She looks at you for a long moment.) You found my lost pages. And you found that. Thank you. I don't know how to thank you.",
    O("I'm glad I could.", "pages_choice", do=["take:page_1", "take:page_2", "take:page_3", "set:ione_quest_done", "rel:ione:2"]))
d.n("pages_choice", "(She stands up very straight and folds her hands, like she's about to give a lecture, and then doesn't.) Can I ask you something unscholarly? What... are we? You and me?",
    O("I'd like us to be more than friends. If you want that too.", "romance", cond="ione_spark"),
    O("You're a dear friend, Ione. One of my best.", "friend"))
d.n("romance", "(She makes a small sound and sits down again.) Yes. I mean — yes, I'd like that. I've drafted this conversation eleven times in my head and none of the drafts were this good. Will you come up to the Archive roof tonight? I want to show you the stars properly. I've a telescope.",
    O("I'll be there.", "end", do=["set:ione_romance", "set:ione_date_set"]),
    O("I'll be there. (Wait for nightfall.)", "end", do=["set:ione_romance", "set:ione_date_set", "event:wait_night:The lamps of Gullhaven come on one by one..."]))
d.n("friend", "(She smiles, and it's only a little bit sad, and then it's not sad at all.) Best friends. I'll write it down. In ink. The permanent kind. Wren will be furious that I've got a best friend before her.",
    O("I'm pretty sure Wren has Sol.", "end", do=["set:ione_bestfriend", "rel:ione:1"]))
d.n("date_remind", "Tonight, on the roof. There's a ladder up through the top floor of the Archive. Bring nothing; I've brought everything. Including snacks. I made a list of snacks.",
    O("I'll go once it's dark.", "hub"),
    O("Let's not wait. (Pass the time until night.)", "end", do=["event:wait_night:The lamps of Gullhaven come on one by one..."]))

# ---------------------------------------------------------------- the date
d.n("date", "(Ione is on the Archive roof with a brass telescope, a blanket, and an alarming quantity of biscuits.) You came. Sit — careful, that's the good blanket. Look up. The fog is thinnest straight above; you can see the stars from here, if you're patient.",
    O("(Sit beside her and look up.)", "date2"))
d.n("date2", "That one is the Heron. And there, the Stag, with the bright star in its antler. The Lanternwrights named them. (She traces them for you with an inky finger.) Every star chart on the mainland calls them something else. Here, we kept the old names. Even forgetting everything else, we kept those.",
    O("Like the lullaby.", "date3"))
d.n("date3", "Exactly like the lullaby. (She's quiet a while.) Can I confess something? Since you came, I've written about you every night in my Book of Days. Pages and pages. I told myself it was for the record. It isn't. I just couldn't bear the idea of forgetting you.",
    O("You won't forget me.", "date4"))
d.n("date4", "(She turns to you, starlight on her glasses.) I like you. I like you so much it's become a research topic. So I'm going to be very brave, for one sentence, and ask: would you like to be with me?",
    O("Yes, Ione. I'd love to be with you.", "yes", cond="dating == 0"),
    O("Before I answer — I'm already with someone. You should know that.", "poly", cond="dating >= 1"),
    O("I think we're better as friends.", "date_friend"))
d.n("yes", "(She kisses you, and it's careful and certain at the same time, and her glasses go crooked.) Good. That's — very good. That's going in the Book of Days in capital letters. (She settles against your shoulder with a biscuit.) Stay till the Heron sets?",
    O("(Stay with her.)", "end", do=["set:ione_dating", "set:ione_date_done", "rel:ione:2"]))
d.n("poly", "(She takes off her glasses and polishes them, which you've learned means she's thinking very hard.) I know. I keep records. You've been seen at the glade, and at the pier at sunset, and frankly the whole village has opinions. (She puts the glasses back on.) Thank you for telling me yourself.",
    O("I'd never want you to hear it from anyone else.", "poly2"))
d.n("poly2", "I've thought about this. Systematically. I don't need to be the only one. I need to be honest with you, and you with me, and all of us with each other. I'll talk to them. We'll have a proper conversation, with an agenda. (A small smile.) I'm going to make an agenda.",
    O("Then, knowing all of it — will you be with me?", "yes_poly"))
d.n("yes_poly", "Knowing all of it. Yes. (She kisses you, then laughs, flustered.) I'm going to need a bigger Book of Days. Stay till the Heron sets?",
    O("(Stay with her.)", "end", do=["set:ione_dating", "set:ione_date_done", "set:ione_poly_ack", "set:poly_open", "rel:ione:2"]))
d.n("date_friend", "(She nods, and pushes the biscuits toward you.) Then we'll be friends who look at stars. That's a very good kind of friend. The best kind, possibly. I'll check the literature.",
    O("(Stay and watch the stars with her.)", "end", do=["set:ione_bestfriend", "set:ione_date_done", "clear:ione_romance"]))
d.n("poly_ack", "(Ione hands you a sheet of paper titled AGENDA, with four items crossed out.) We talked. All of us. It went better than my contingency plans. Wren said 'fine' eleven times, which I've learned means she's happy. Sol cried, then denied it. So did I.",
    O("And you're all right?", "poly_ack2"))
d.n("poly_ack2", "I'm more than all right. I'm loved, and I know exactly where I stand, and nobody's hiding anything. That's all I ever wanted from anyone. (She tucks the agenda into her Book of Days.) For the record.",
    O("For the record.", "hub", do=["set:ione_poly_ack"]))

# ---------------------------------------------------------------- girlfriend + after
d.n("gf", "(She glows, then remembers she's at work and tries to look professional, which lasts about a second.) Hello. I was just writing about you. Not in a strange way. In a normal, devoted, slightly strange way.",
    O("What did you write?", "gf2"),
    O("I'll let you get back to it.", "end"))
d.n("gf2", "(She hides the page against her chest.) Classified. You can read it in fifty years, when we're old and I've forgotten everything except you. (She kisses your cheek.) Now go and do something heroic so I've got more to write.",
    O("Yes, ma'am.", "end"))
d.n("after", "(Ione is sitting on the Archive steps, her Book of Days unopened on her knees.) I didn't read it this morning. I woke up and I just... remembered yesterday. All on my own. (She laughs, astonished.) Do you know how long it's been since I trusted my own head?",
    O("You can trust it now.", "hub", do=["set:ione_after", "rel:ione:1"]))

# ---------------------------------------------------------------- epilogues
d.n("epi", "(Ione has a lantern in one hand and her Book of Days in the other, and for once she isn't writing.)",
    O("...", "epi_dating", cond="ione_dating"),
    O("...", "epi_friend", cond="!ione_dating && (ione_bestfriend || rel.ione >= 4)"),
    O("...", "epi_other", cond="!ione_dating && !ione_bestfriend && rel.ione < 4"))
d.n("epi_dating", "There you are. I've decided to start a new Chronicle. Volume one: 'The year the island was remembered.' You're on every page. (She stands on tiptoe and kisses you.) Dance with me? I'm terrible at it. I've read about it extensively.",
    O("(Dance with Ione.)", "end", do=["set:ione_epi", "rel:ione:1"]))
d.n("epi_friend", "My best friend! Come here. I've written you into the Chronicle as 'the friend who stayed'. It's a very good entry. Footnotes and everything.",
    O("I'm honoured.", "end", do=["set:ione_epi"]))
d.n("epi_other", "Thank you. For everything. I'm going to write it all down — properly, so no one ever forgets again. Come by the Archive some time. I'll read you the good parts.",
    O("I will.", "end", do=["set:ione_epi"]))
d.n("epi_all", "(Wren, Ione and Sol are standing together by the well, and all three of them turn as you arrive. Sol is grinning, Ione is blushing, and Wren is trying very hard to look unimpressed.) Ione: 'Oh good. You're here. We've prepared remarks.'",
    O("...", "epi_all2"))
d.n("epi_all2", "Sol: 'We've decided you're not allowed to save any more islands without us.' Ione: 'It's in writing. I wrote it.' Wren: 'Shut up and come here.' (They pull you into the middle of them, and the lantern beam sweeps over all four of you.)",
    O("I love you. All of you.", "epi_all3"))
d.n("epi_all3", "(Ione's voice is very small and very sure.) We love you too. We discussed it. At length. There was an agenda. Now dance with us, before the music stops.",
    O("(Dance with them under the lantern.)", "end", do=["set:wren_epi", "set:ione_epi", "set:sol_epi", "set:all_three_scene"]))

d.bark(None, "(scritch scritch scritch)")
d.bark("met.ione", "Hm. Footnote four is wrong.")
d.bark("!lantern_lit", "Write it down or lose it.")
d.bark("ione_dating", "(Ione waves, then pretends she didn't.)")
d.bark("lantern_lit", "I remembered breakfast. On my own!")
