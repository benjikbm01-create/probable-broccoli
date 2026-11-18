from dsl import D, O
d = D("tobias")
d.entry("festival && !tobias_epi", "fest")
d.entry("festival", "fest_after")
d.entry("lantern_lit && !tobias_remembers", "remember")
d.entry("has:logbook && tobias_asked && !side_tobias_done", "log_back")
d.entry("!met.tobias", "first")
d.entry(None, "hub")

d.n("first", "Well, I'll be keelhauled. A boat. Out of the fog. Out of the actual fog.",
    O("Hello! Is this Gullhaven?", "first2"),
    O("You look like you've seen a ghost.", "first2"))
d.n("first2", "Gullhaven, aye, on the Isle of Ysmere. Tobias Wake, harbourmaster. Thirty years I've kept this pier, and in thirty years nothing has come in out of that grey. Not a gull's feather.",
    O("I came because of a letter.", "letter"),
    O("Thirty years? Why?", "fog1"))
d.n("fog1", "The fog. Sail into it and you come round again to this very pier, with no memory of turning. People tried. Families tried. Folk stopped trying.",
    O("Then how did I get here?", "fog2"))
d.n("fog2", "That's the question, isn't it. Whatever let you in, I'd like to shake its hand. Now — you said something about a letter?",
    O("Show him the letter.", "letter"))
d.n("letter", "(He turns the salt-stained page over in thick fingers.) 'E.V.' Never heard of anyone by those letters. And this seal — a bird. A kestrel. I know that bird... (He frowns, and the knowing slides away from him.) No. Gone. That's how it goes here.",
    O("What do you mean, 'gone'?", "letter2"))
d.n("letter2", "Things go, on Ysmere. Names. Faces. Why you walked into a room. Some days it's like the fog's in your head. Listen: get yourself to the Gull & Lantern, up past the square. Hester will give you a bed and a hot meal.",
    O("And the letter?", "letter3"))
d.n("letter3", "Show it to Ione at the Archive. She collects old seals and older words, and she writes everything down so it can't wander off. Welcome to Gullhaven, stranger. Mind the gulls; they steal.",
    O("Thank you, Tobias.", "end", do=["set:shown_letter_tobias", "rel:tobias:1"]))

d.n("hub", "Harbour's quiet. Always is. What can I do for you?",
    O("Tell me about the fog.", "fog_lore"),
    O("Who should I talk to about the letter?", "letter_again", cond="!letter_identified"),
    O("What do you know about Kestrel Keep?", "keep", cond="letter_identified"),
    O("You look like something's weighing on you.", "quest", cond="letter_identified && !tobias_asked"),
    O("About your logbook...", "quest_remind", cond="tobias_asked && !side_tobias_done && !has:logbook"),
    O("How do you know Sol?", "sol", cond="met.sol"),
    O("I'm going up to the Keep.", "keep_warn", cond="puzzle1_solved && !saw_empty_lantern"),
    O("Good day, Tobias.", "end"))
d.n("fog_lore", "Grey as a gull's back, cold as a widow's bed. It sits a mile out all round the island and never moves, not for wind, not for tide. Old Wendel says it came the winter of the great storm. I don't remember that winter. Funny, that. I remember every winter but that one.",
    O("Back to other things.", "hub"))
d.n("letter_again", "Ione, at the Archive — the tall stone place west of the square, can't miss it. If anyone can read a seal, it's her.",
    O("Thanks.", "hub"))
d.n("keep", "The Keep? Up on the north cliffs. Dead and dark since before I went grey. There was a road once, through the Mosswood. The thorns had it long ago. Wren — the ranger — might know more. She knows every root in that forest.",
    O("Back to other things.", "hub"))
d.n("keep_warn", "Then take care. There's a cold up there that isn't weather. Come back down and tell me the lantern's just a lump of glass, eh? I'd sleep better.",
    O("I'll be careful.", "hub"))
d.n("sol", "Sol? I taught her to splice a rope when she was knee-high to a bollard. Best hands on the island and the worst patience. She's building a boat to beat the fog. I keep telling her the fog doesn't lose.",
    O("Do you think she can do it?", "sol2"))
d.n("sol2", "(He looks at the grey horizon for a long while.) I think if anyone could, it'd be her. Don't tell her I said so. She'd be insufferable.",
    O("Your secret's safe.", "hub", do=["rel:tobias:1"]))
d.n("quest", "(He scratches his beard.) You've a sharp eye. It's my old logbook. My boat, the Meridian, went down in the bay the night of the great storm, and the log went with her. Everything I wrote that night is down there.",
    O("Why does it matter so much?", "quest2"))
d.n("quest2", "Because I don't remember that night. Not one minute. I went out in the worst storm in living memory, lost my boat, nearly drowned — and I don't know why. A man ought to know why he nearly died.",
    O("I'll find your log.", "quest3"),
    O("Maybe another time.", "hub"))
d.n("quest3", "Would you? She lies west of the long pier, a stone's throw out, in deep water. You'll want a lungful of something better than air. Hester brews a Diver's Draught for the kelp-cutters. Ask her for one.",
    O("I'll ask Hester.", "end", do=["set:tobias_asked"]))
d.n("quest_remind", "West of the long pier, in deep water. Look for the broken mast. The log would be in my cabin at the stern, in the sea-chest. And mind you ask Hester for her Draught first.",
    O("Got it.", "hub"))
d.n("log_back", "Is that — you found it. The Meridian's log. (He opens it with trembling care and reads for a long time, his lips moving.)",
    O("What does it say?", "log_back2"))
d.n("log_back2", "'Warden's wife and girl lost off the harbour wall. Taking the Meridian out. God help us.' (He closes the book.) I went out to search for them. For a mother and a little girl. And I can't remember their faces. I can't even remember the Warden's name.",
    O("You tried to save them. That's what matters.", "log_back3"),
    O("Who was the Warden?", "log_back3"))
d.n("log_back3", "Thank you. You've given me back a piece of myself, and I'd not have thought that possible. Here. My old spyglass. It's seen more horizon than I have lately. Maybe it'll see more for you.",
    O("Thank you, Tobias.", "end", do=["take:logbook", "give:spyglass", "set:side_tobias_done", "rel:tobias:2"]))
d.n("remember", "(Tobias stands at the end of the pier, staring at the beam of light over the cliffs.) I remember. Edric Vael. Warden of Kestrel Keep. My friend. We drank in the Gull & Lantern every Friday for twenty years, and I forgot his name.",
    O("What else do you remember?", "remember2"))
d.n("remember2", "The storm. Annis on the harbour wall with little Elsie. The wave. I took the Meridian out and searched till she broke under me. And Edric on the quay at dawn, white as salt, with a light in his hands. (He wipes his eyes.) Old fool. Go on. Gullhaven owes you more than it can say.",
    O("Take care, Tobias.", "end", do=["set:tobias_remembers", "rel:tobias:1"]))
d.n("fest", "Look at it! Lanterns in the square, music, and the fog gone like it was never there. I can see the line of the mainland, did you know? Faint as a pencil mark. I'd forgotten it was there at all.",
    O("It's beautiful.", "fest2"))
d.n("fest2", "It is. Thirty years, and a stranger comes in out of the grey and fixes it in a fortnight. I'll be lighting the harbour lamps at the end of the pier later, if you've a mind to watch. Find your people first.",
    O("I will.", "end", do=["set:tobias_epi"]))
d.n("fest_after", "Clear skies, and ships to come. I'll have to remember how to be a proper harbourmaster. Paperwork. Shudder.",
    O("You'll manage.", "end"))

d.bark("!lantern_lit", "Grey again. Always grey.")
d.bark("!lantern_lit", "Hm. Gulls are restless today.")
d.bark("met.tobias && !lantern_lit", "Don't go rowing into that, now.")
d.bark("lantern_lit", "Would you look at that horizon.")
d.bark("side_tobias_done", "Edric... no. Gone again. Bah.")
