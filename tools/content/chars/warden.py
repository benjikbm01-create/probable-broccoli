from dsl import D, O
d = D("warden")
S = "Edric Vael"
d.entry("!warden_rest", "w1")
d.entry(None, "done")
d.n("done", "(The Wellspring is quiet now. Only your own reflection looks back from the water.)", O("(Leave.)", "end"), speaker="The Wellspring")

d.n("w1", "(The drowned shape kneels in the shallows. The water runs off it, and underneath is only a tired man in a Warden's coat, older than he should be, with salt in his beard.) ...You took it from me. The flame. I begged you not to. I wrote that I would.",
    O("You wrote the letter.", "w2"), speaker=S)
d.n("w2", "Every winter, when my mind cleared for an hour, I wrote one and gave it to the tide. Thirty years of letters. Only one found anyone. (He laughs, and it's a cough.) Edric Vael. Warden of Kestrel Keep. I'd forgotten my own name, down here, until you said it with your sword.",
    O("Why did you take the Ember?", "w3"), speaker=S)
d.n("w3", "Because the sea took my wife and my little girl, and the walls said the Wellspring keeps what the sea takes. I could hear them in the water. Annis. Elsie. I thought if I stayed, if I held the light close enough, I could bring them up. (He looks at the Ember in the water.) I let the whole island fade, to keep two voices.",
    O("Your daughter didn't drown. She lived.", "told", cond="knows_hester"),
    O("They're at peace, Edric. You can rest now.", "rest"), speaker=S)
d.n("told", "(He goes absolutely still.) ...What?",
    O("Maud Pike found her on the shore. She grew up at the Gull & Lantern. She's called Hester now.", "told2"), speaker=S)
d.n("told2", "(He makes a sound you will remember for the rest of your life.) Elsie. Alive. All this time, in the village, pouring beer — (He laughs and weeps at once.) And I was down here, listening to an echo of her. Tell her — tell her the song was mine. I wrote it for her when she was born. Tell her I'm sorry, and that I'm proud of her. Tell her I'm going to find her mother.",
    O("I'll tell her. I promise.", "end_told"), speaker=S)
d.n("end_told", "(He lifts the Ember out of the water and places it in your hands. It's warm as a held hand.) Take it home. Light the lantern. Let the world remember Ysmere — and let her remember me. (He smiles, and the water takes him, gently, and he's gone.)",
    O("(Hold the Ember.)", "end", do=["set:told_warden", "event:warden_rest"]), speaker=S)
d.n("rest", "Peace. (He closes his eyes.) I'd like that. I'm so tired. (He lifts the Ember out of the water and places it in your hands.) Take it home. Light the lantern. Let the world remember Ysmere. (The water takes him, gently, and he's gone.)",
    O("(Hold the Ember.)", "end", do=["event:warden_rest"]), speaker=S)
