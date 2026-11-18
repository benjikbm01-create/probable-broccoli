def E(fr, to, at, act, r=0):
    e = {"from": fr, "to": to, "at": at, "act": act}
    if r: e["r"] = r
    return e

FEST = lambda spot: {"if": "festival && !credits_seen", "entries": [E(0, 24000, spot, "stand")]}

SCHEDULES = {
  "hester": {"entries": [E(0, 3000, "hester_kitchen", "work"), E(3000, 7000, "hester_bar", "work"), E(7000, 9000, "hester_tea", "sit"),
                         E(9000, 10500, "inn_garden", "wander", 4), E(10500, 16500, "hester_bar", "work"), E(16500, 23500, "hester_bed", "sleep"),
                         E(23500, 24000, "hester_kitchen", "work")],
             "overrides": [FEST("festival_hester"), {"if": "lantern_lit && !festival", "entries": [E(0, 24000, "inn_door", "stand")]}]},
  "tobias": {"entries": [E(0, 2000, "tobias_office", "work"), E(2000, 6000, "tobias_pier", "stand"), E(6000, 8000, "harbor_front", "wander", 5),
                         E(8000, 11000, "tobias_tower", "stand"), E(11000, 14000, "tobias_inn", "sit"), E(14000, 24000, "tobias_bed", "sleep")],
             "overrides": [{"if": "intro_done && !met.tobias", "entries": [E(0, 24000, "tobias_greet", "stand")]}, FEST("festival_tobias"),
                           {"if": "lantern_lit && !festival", "entries": [E(0, 24000, "pier_end", "stand")]}]},
  "wendel": {"entries": [E(0, 4000, "wendel_shore", "wander", 4), E(4000, 7000, "wendel_bench", "sit"), E(7000, 9000, "maud_grave", "stand"),
                         E(9000, 12000, "wendel_pier", "stand"), E(12000, 14000, "wendel_home", "stand"), E(14000, 24000, "wendel_bed", "sleep")],
             "overrides": [FEST("festival_wendel")]},
  "corin": {"entries": [E(0, 1000, "corin_home", "stand"), E(1000, 6000, "corin_anvil", "work"), E(6000, 7000, "corin_garden", "wander", 3),
                        E(7000, 11000, "corin_anvil", "work"), E(11000, 14500, "corin_inn", "sit"), E(14500, 24000, "corin_bed", "sleep")],
            "overrides": [FEST("festival_corin")]},
  "wren": {"entries": [E(0, 3000, "wren_patrol_s", "wander", 6), E(3000, 6000, "wren_lodge", "work"), E(6000, 8000, "wren_range", "work"),
                       E(8000, 9500, "smithy_front", "talk"), E(9500, 11000, "wren_patrol_n", "wander", 8), E(11000, 14000, "wren_inn", "sit"),
                       E(14000, 24000, "wren_bed", "sleep")],
           "overrides": [FEST("festival_wren"),
                         {"if": "wren_date_set && !wren_date_done", "entries": [E(13000, 23000, "glade_date", "sit")]},
                         {"if": "wren_quest && !wren_quest_done", "entries": [E(3000, 8000, "hollow_oak", "stand")]}]},
  "ione": {"entries": [E(0, 4000, "ione_desk", "work"), E(4000, 6000, "ione_shelves", "wander", 3), E(6000, 7000, "stall_odds", "stand"),
                       E(7000, 9000, "ione_tea", "sit"), E(9000, 12000, "ione_desk", "work"), E(12000, 16000, "archive_roof", "stand"),
                       E(16000, 24000, "ione_bed", "sleep")],
           "overrides": [FEST("festival_ione"), {"if": "ione_date_set && !ione_date_done", "entries": [E(12000, 23000, "archive_roof", "stand")]}]},
  "sol": {"entries": [E(0, 5000, "sol_hull", "work"), E(5000, 6500, "sol_pier", "stand"), E(6500, 8000, "stall_cloth", "stand"),
                      E(8000, 11000, "sol_hull", "work"), E(11000, 14000, "sol_inn", "sit"), E(14000, 24000, "sol_bed", "sleep")],
          "overrides": [{"if": "sol_sailing", "entries": [E(0, 24000, "date_gullrock_sol", "sit")]}, FEST("festival_sol"),
                        {"if": "sol_date_set && !sol_date_done", "entries": [E(11000, 14500, "sol_pier", "stand")]}]},
}

def C(a, b, cond, *lines):
    c = {"a": a, "b": b, "lines": list(lines)}
    if cond: c["if"] = cond
    return c

CHATS = [
  C("wren", "sol", "!lantern_lit", "sol: Another round?", "wren: You said that two rounds ago.", "sol: And I was right then too.", "wren: ...Fine. One more."),
  C("wren", "sol", "met.wren && met.sol && !lantern_lit", "wren: The stranger asked about the stones.", "sol: Ooh. Are you going to be nice to them?", "wren: I'm always nice.", "sol: You shot Tobias's hat."),
  C("sol", "wren", "sol_quest && !sol_quest_done", "sol: Did you cut me that spar or not?", "wren: I cut it. I left it by the fallen pine.", "sol: Two years ago?", "wren: ...Possibly."),
  C("hester", "ione", "!lantern_lit", "ione: I found another blank page today.", "hester: More tea, love. You can't fight the fog on an empty cup.", "ione: That's not how it works.", "hester: Drink it anyway."),
  C("ione", "hester", "letter_identified", "ione: A letter, Hester. From the Keep.", "hester: E.V. I keep turning it over in my head.", "ione: Me too. Like a word on the tip of my tongue."),
  C("tobias", "sol", "!lantern_lit", "tobias: Wind's backing.", "sol: Good. I'm taking her out tomorrow.", "tobias: You'll come round the pier by noon, same as always.", "sol: Then I'll try again by one."),
  C("corin", "wren", None, "wren: These arrowheads are too heavy.", "corin: Last week they were too light.", "wren: They were too light last week.", "corin: Give me patience."),
  C("tobias", "corin", None, "corin: Evening, Tobias.", "tobias: Is it? Fog's so thick I can't tell.", "corin: That's what the cider's for."),
  C("wren", "ione", "poly_open && wren_dating && ione_dating", "wren: You made an agenda.", "ione: Agendas are how civilised people have feelings.", "wren: ...It was a good agenda.", "ione: Thank you."),
  C("sol", "ione", "poly_open && sol_dating && ione_dating", "sol: You're blushing.", "ione: I'm not. It's the fire.", "sol: We're outside.", "ione: It's a very large fire."),
  C("hester", "tobias", "lantern_lit", "tobias: Elsie. You used to sit on my knee and steal my pipe.", "hester: I remember. You let me.", "tobias: I let you do everything. Your father despaired."),
  C("wren", "sol", "lantern_lit", "sol: When the Gull's Wing sails, you're coming.", "wren: Who'll keep the forest?", "sol: The trees can wait a week. Come on, Wren.", "wren: ...A week."),
]
