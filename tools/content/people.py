def P(npc, name, role, *lines):
    return {"npc": npc, "name": name, "role": role, "lines": [{"if": c, "text": t} if c else {"text": t} for c, t in lines]}

PEOPLE = [
    P("hester", "Hester Moss", "Keeper of the Gull & Lantern inn.",
      ("festival", "She remembers something important about herself."), ("knows_song", "She taught you an old lullaby she has hummed all her life."), (None, "Warm, busy, and kind to strangers.")),
    P("tobias", "Tobias Wake", "Harbourmaster of Gullhaven.",
      ("side_tobias_done", "You returned his lost logbook."), (None, "Gruff, weathered, and quietly watchful of the fog.")),
    P("ione", "Ione Calder", "Keeper of the Archive.",
      ("ione_dating", "You're together. She writes about you every night."), ("ione_bestfriend", "One of your closest friends."), ("ione_quest_done", "You found her missing pages."), (None, "She writes everything down so nothing is lost.")),
    P("wren", "Wren Ashdown", "Ranger of the Mosswood.",
      ("wren_dating", "You're together. She pretends not to blush."), ("wren_bestfriend", "One of your closest friends."), ("wren_quest_done", "You brought back her mother's bow."), (None, "Blunt, sharp-eyed, and more caring than she lets on.")),
    P("sol", "Sol Reyes", "Boatwright at the harbour.",
      ("sol_dating", "You're together. She tells the gulls about you."), ("sol_bestfriend", "One of your closest friends."), ("sol_quest_done", "You helped finish the Gull's Wing."), (None, "Loud, bold, and desperate to see what lies past the fog.")),
    P("corin", "Corin Ashby", "Smith and farrier.",
      ("side_corin_done", "He forged you the Kestrel Blade."), (None, "Cheerful, soot-covered, and missing his cat.")),
    P("wendel", "Wendel Pike", "Retired fisherman on the western shore.",
      ("side_wendel_done", "You relit his late wife's waystone lamps."), (None, "Forgetful, wry, and wiser than he looks. Ask him if you're stuck.")),
]

SECRETS = [
    {"id": "smugglers_cache", "name": "Smugglers' Cache", "hint": "Something hangs beneath the fishing pier."},
    {"id": "inn_cellar", "name": "The Mosses' Hidden Room", "hint": "Even an inn cellar can keep a secret."},
    {"id": "belltower", "name": "The Bell Tower", "hint": "High above the chapel."},
    {"id": "windmill", "name": "The Miller's Loft", "hint": "Where the sails turn."},
    {"id": "falls_cave", "name": "Behind the Falls", "hint": "Water can hide a door."},
    {"id": "watcher_nest", "name": "The Watcher's Nest", "hint": "A tree taller than the rest."},
    {"id": "gull_rock", "name": "Top of Gull Rock", "hint": "The sea stack off the harbour."},
    {"id": "cliff_cave", "name": "The Cliff Cave", "hint": "Below the Keep's western wall."},
    {"id": "keep_vault", "name": "The Armoury Vault", "hint": "A cask with a button on it."},
    {"id": "flooded_chamber", "name": "The Flooded Chamber", "hint": "Hold your breath in the Hall of Echoes."},
    {"id": "kings_mural", "name": "The King's Mural", "hint": "A crack in an old wall."},
    {"id": "cairn", "name": "The Summit Cairn", "hint": "The highest point of the forest."},
]
