package dev.voidoutlaws.game;

import dev.voidoutlaws.menu.ChoiceView;
import dev.voidoutlaws.menu.KioskView;
import dev.voidoutlaws.menu.Menu;
import dev.voidoutlaws.reg.ModItems;
import dev.voidoutlaws.state.CrewState;
import dev.voidoutlaws.state.Job;
import dev.voidoutlaws.world.Loc;
import dev.voidoutlaws.world.Structures;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

/** The short campaign. (Developers: see SPOILERS.md. Players: no peeking!) */
public final class Story {
    private Story() {}

    public static final int TUTORIAL = 0, MEET = 1, DEBT = 2, EGGS = 3, EGGS_DONE = 4, VAULT = 5, LEDGER = 6, AMBUSH = 7, FREEPLAY = 8;

    private static final Map<UUID, Long> TALK_CD = new HashMap<>();

    public static String goal() {
        CrewState s = Game.S();
        return switch (s.story) {
            case TUTORIAL -> "Finish the tutorial (/tutorial)";
            case MEET -> "Visit the Baroness (Gilded Maw)";
            case DEBT -> s.storyCounter >= 2 ? "Report to the Baroness" : "Finish 2 jobs (" + s.storyCounter + "/2)";
            case EGGS -> "Board the derelict, get the eggs";
            case EGGS_DONE -> "Talk to the Baroness";
            case VAULT -> "Rob the Lumen Vault (Meridian)";
            case LEDGER -> "Decide the Ledger's fate";
            case AMBUSH -> "Survive! (Gilded Maw)";
            default -> "Freeplay: rob the galaxy!";
        };
    }

    public static boolean storyHunterPending() {
        return Game.S().story == AMBUSH;
    }

    private static void advance(int stage) {
        CrewState s = Game.S();
        if (s.story >= stage) return;
        s.story = stage;
        s.storyCounter = 0;
        s.markDirty();
        Game.crew(Game.t("New goal: ", Formatting.LIGHT_PURPLE).append(Game.t(goal(), Formatting.WHITE)));
        Game.sound(Game.players(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.8f);
    }

    // ------------------------------------------------------------------ hooks
    public static void onTutorialDone() {
        advance(MEET);
    }

    public static void onJobComplete(Job j) {
        CrewState s = Game.S();
        if (s.story == TUTORIAL) { advance(MEET); return; }
        if (s.story == DEBT) {
            s.storyCounter++;
            s.markDirty();
            Game.crew(Game.t("Story: ", Formatting.LIGHT_PURPLE).append(Game.t(goal(), Formatting.WHITE)));
        }
        if (j.story && j.type == Job.Type.BOARDING && s.story == EGGS) {
            advance(EGGS_DONE);
            for (ServerPlayerEntity p : Game.players()) giveWhistle(p);
            Game.crew("The Baroness lets the crew keep a hatchling as a 'bonus'. It has bonded with EVERYONE. Use your Companion Whistle!", Formatting.LIGHT_PURPLE);
        }
    }

    public static void giveWhistle(ServerPlayerEntity p) {
        if (Game.hasItem(p, ModItems.COMPANION_WHISTLE)) return;
        p.getInventory().offerOrDrop(new ItemStack(ModItems.COMPANION_WHISTLE));
    }

    public static void onJoin(ServerPlayerEntity p) {
        CrewState s = Game.S();
        if (s.story >= EGGS_DONE && !s.companion.containsKey(p.getUuid())) giveWhistle(p);
    }

    public static void onVaultLooted(ServerPlayerEntity p) {
        CrewState s = Game.S();
        Job j = s.job;
        if (s.story == VAULT && j != null && j.story) {
            p.getInventory().offerOrDrop(Books.ledger());
            Game.crew(Game.name(p) + " found an odd ledger in the vault, with the Baroness's seal on it. Read it! (right-click)", Formatting.LIGHT_PURPLE);
            advance(LEDGER);
        }
    }

    static void onArrive(MinecraftServer server, Loc dest) {
        CrewState s = Game.S();
        if (s.story == AMBUSH && dest == Loc.GILDED_MAW) Sched.later(80, Story::spawnRook);
    }

    private static void spawnRook() {
        CrewState s = Game.S();
        if (s.story != AMBUSH || s.shipLoc != Loc.GILDED_MAW || s.jumping) return;
        ServerWorld w = Game.server.getWorld(Loc.GILDED_MAW.key);
        if (w == null) return;
        if (w.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            Game.crew("(Peaceful difficulty: the hunter takes one look at your crew and flees in terror.)", Formatting.GRAY);
            onStoryHunterDefeated();
            return;
        }
        Mobs.spawnHunter(w, Structures.origin(s, Loc.GILDED_MAW).add(0, 0, -4), "Rook Venn", true);
    }

    public static void onStoryHunterDefeated() {
        CrewState s = Game.S();
        if (s.story != AMBUSH) return;
        s.pot += 500;
        s.ending = "loyal";
        s.markDirty();
        line("baroness", "Rook... lost? To THEM? ...Fine. FINE. Take your payment, darlings, and let's pretend this never happened.");
        finale("Loyal to a Fault");
    }

    private static void finale(String ending) {
        CrewState s = Game.S();
        s.story = FREEPLAY;
        s.markDirty();
        Sched.later(60, () -> {
            Game.title(Game.players(), Game.t("THE END", Formatting.GOLD), Game.t("Ending: " + ending, Formatting.WHITE));
            Game.sound(Game.players(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            Game.crew("Thanks for playing Void Outlaws! The galaxy is still yours to rob: heists and boarding jobs now show up on the job boards.", Formatting.GOLD);
        });
    }

    // ------------------------------------------------------------------ dialogue
    private static void line(String npc, String text) {
        Npcs.Def d = Npcs.byId(npc);
        Text t = Text.literal("<" + (d == null ? npc : d.name()) + "> ").formatted(d == null ? Formatting.WHITE : d.color())
                .append(Text.literal(text).formatted(Formatting.WHITE));
        for (ServerPlayerEntity p : Game.players()) {
            if (d == null || p.getServerWorld().getRegistryKey().equals(d.loc().key)) p.sendMessage(t);
        }
    }

    private static void say(String npc, String... lines) {
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i];
            Sched.later(1 + i * 35, () -> line(npc, l));
        }
    }

    private static int sayDelay(int lines) {
        return 1 + lines * 35;
    }

    public static void talk(ServerPlayerEntity p, String npc) {
        long now = Game.tick;
        Long cd = TALK_CD.get(p.getUuid());
        if (cd != null && now - cd < 40) return;
        TALK_CD.put(p.getUuid(), now);
        Game.sound(p, SoundEvents.ENTITY_VILLAGER_AMBIENT, 1f, 1f);
        switch (npc) {
            case "mags" -> talkMags(p);
            case "baroness" -> talkBaroness(p);
            case "pell" -> talkPell(p);
            default -> {}
        }
    }

    private static final String[] MAGS_TIPS = {
            "Heat too high? Hole up in your Burrow for a while. Patrols forget faces fast.",
            "Scanners can't see into a Hidden Compartment. Mostly. Unless you're famous.",
            "Bribes work better when you're NOT on every wanted poster in the sector.",
            "Got a grudge in your crew? Buy everyone a round at my card table. Fixes everything.",
            "A Transponder Chip gives your ship a brand-new name. The cops hate that one trick.",
            "Pirates hate a good gunner. And a pilot who can actually steer.",
            "Disguise kits: a fake mustache is legally a different person on Meridian. Look it up."
    };

    private static void talkMags(ServerPlayerEntity p) {
        CrewState s = Game.S();
        if (s.story == TUTORIAL || Tutorial.step(p) >= 0) {
            say("mags", "Well, look at you! Fresh crew, shiny ship. Well. Ship.",
                    "Grab a job off the JOB BOARD by the door, hon. The first one's an easy run.");
            return;
        }
        if (s.story == MEET) {
            say("mags", "The Baroness on the Gilded Maw has been asking about your ship. Asking LOUDLY.",
                    "Go see her. Bring manners. Leave your blasters holstered.");
            return;
        }
        say("mags", MAGS_TIPS[Game.R.nextInt(MAGS_TIPS.length)]);
    }

    private static void talkBaroness(ServerPlayerEntity p) {
        CrewState s = Game.S();
        switch (s.story) {
            case TUTORIAL, MEET -> {
                say("baroness", "Darlings! So YOU are the crew flying my ship.",
                        "Oh, you didn't know? The Lucky Larceny was bought with MY money. Consider yourselves... employed.",
                        "Finish two jobs - any jobs - and we'll call the down payment settled.");
                if (s.story == TUTORIAL) { s.story = MEET; }
                Sched.later(sayDelay(3), () -> advance(DEBT));
            }
            case DEBT -> {
                if (s.storyCounter < 2) {
                    say("baroness", "Two jobs, darlings. You've done " + s.storyCounter + ". I can count, you know. I count EVERYTHING.");
                    return;
                }
                if (s.job != null) {
                    say("baroness", "Debt paid! Lovely. Now finish whatever little errand you're running, then come back. I have a real job.");
                    return;
                }
                say("baroness", "Debt paid! I'm almost proud.",
                        "There's a derelict freighter drifting past the Maw, full of Grumbleback eggs. Bring me three.",
                        "Scavengers are crawling all over it. Do try not to die, I'd have to find a new crew.");
                Sched.later(sayDelay(3), () -> {
                    if (Game.S().job == null && Game.S().story == DEBT) {
                        Jobs.accept(p, Jobs.boarding(true), Loc.GILDED_MAW);
                        advance(EGGS);
                    }
                });
            }
            case EGGS -> say("baroness", "Eggs, darlings. Three of them. The Derelict Freighter is on your star map. Deliver them at my Job Board.");
            case EGGS_DONE -> {
                if (s.job != null) {
                    say("baroness", "Finish your current errand first, darlings. Then we talk about the BIG one.");
                    return;
                }
                say("baroness", "Lovely eggs. Now - the big one.",
                        "The Lumen Trust bank on Meridian Prime keeps a certain ledger in its vault. I want it.",
                        "Crack the vault. Take the ledger. Bring it to ME. Don't read it. Reading is for nerds.");
                Sched.later(sayDelay(3), () -> {
                    if (Game.S().job == null && Game.S().story == EGGS_DONE) {
                        Jobs.accept(p, Jobs.heist(true), Loc.GILDED_MAW);
                        advance(VAULT);
                    }
                });
            }
            case VAULT -> say("baroness", "The ledger, darlings. Meridian Prime. The bank vault. Chop chop.");
            case LEDGER -> {
                say("baroness", "Ah! You have my ledger. Hand it over, darlings.");
                Sched.later(sayDelay(1), () -> Menu.open(p, new ChoiceView("What do you do with the Ledger?", List.of(
                        new ChoiceView.Option(Items.WRITABLE_BOOK, "Hand over the Ledger", Formatting.GOLD,
                                "Give the Baroness what she asked for.", () -> handOver(p)),
                        new ChoiceView.Option(Items.EMERALD, "Keep it as 'insurance'", Formatting.GREEN,
                                "You read it. She knows you read it.", () -> blackmail(p)),
                        new ChoiceView.Option(Items.CLOCK, "Not yet...", Formatting.GRAY,
                                "Think about it some more.", () -> {})
                ))));
            }
            case AMBUSH -> say("baroness", "ROOK! Do your JOB!");
            default -> {
                switch (s.ending) {
                    case "clean" -> say("baroness", "I'm under 'house arrest', darlings. In my own station. It's very comfortable. I'm plotting.");
                    case "insurance" -> say("baroness", "Our little secret, darlings. Now go be criminal somewhere else.");
                    default -> say("baroness", "We're even. For now. The job board has work, if you're bored.");
                }
            }
        }
    }

    private static void takeLedger() {
        for (ServerPlayerEntity pl : Game.players()) {
            var inv = pl.getInventory();
            for (int i = 0; i < inv.size(); i++) if (inv.getStack(i).isOf(ModItems.LUMEN_LEDGER)) inv.setStack(i, ItemStack.EMPTY);
        }
        CrewState s = Game.S();
        for (var inv : List.of(s.cargo, s.hidden))
            for (int i = 0; i < inv.size(); i++) if (inv.getStack(i).isOf(ModItems.LUMEN_LEDGER)) inv.setStack(i, ItemStack.EMPTY);
    }

    private static void handOver(ServerPlayerEntity p) {
        CrewState s = Game.S();
        if (s.story != LEDGER) return;
        takeLedger();
        say("baroness", "Thank you, darlings. You were SO useful.",
                "And now... Rook, dear? Collect them. The Concordat pays very well for scapegoats.");
        Sched.later(sayDelay(2), () -> {
            Game.title(Game.players(), Game.t("BETRAYED!", Formatting.DARK_RED), Game.t("Survive the ambush!", Formatting.WHITE));
            Game.S().story = AMBUSH;
            Game.S().markDirty();
            Game.crew(Game.t("New goal: ", Formatting.LIGHT_PURPLE).append(Game.t(goal(), Formatting.WHITE)));
            spawnRook();
        });
    }

    private static void blackmail(ServerPlayerEntity p) {
        CrewState s = Game.S();
        if (s.story != LEDGER) return;
        say("baroness", "...You READ it. Clever little thieves.",
                "Fine. 800 credits, and you never, ever mention it again. Keep your little book. I'll keep my smile.");
        Sched.later(sayDelay(2), () -> {
            CrewState st = Game.S();
            st.pot += 800;
            st.addRep("syndicate", -20);
            st.ending = "insurance";
            st.markDirty();
            Game.crew("+800 credits of hush money in the Crew Pot!", Formatting.GOLD);
            finale("Insurance Policy");
        });
    }

    private static void talkPell(ServerPlayerEntity p) {
        CrewState s = Game.S();
        if (s.story == LEDGER) {
            say("pell", "That's... a Lumen Trust ledger. With the Brightcoil seal. Where did you- no. Don't tell me.");
            Sched.later(sayDelay(1), () -> Menu.open(p, new ChoiceView("Turn in the Ledger?", List.of(
                    new ChoiceView.Option(Items.WRITABLE_BOOK, "Turn in the Ledger", Formatting.AQUA,
                            "Hand the evidence to the law. Yes, really.", () -> turnIn(p)),
                    new ChoiceView.Option(Items.CLOCK, "Never mind", Formatting.GRAY,
                            "Walk away whistling.", () -> {})
            ))));
            return;
        }
        say("pell", s.heat >= 40
                ? "Officer Pell, Customs. Your ship is on my screen a LOT lately. There's a... processing fee to make that go away."
                : "Officer Pell, Customs. Anything to declare? ...Didn't think so. Have a lawful day.");
        Sched.later(sayDelay(1), () -> { if (!p.isRemoved() && Game.locOf(p) == Loc.MERIDIAN) Menu.open(p, new KioskView()); });
    }

    private static void turnIn(ServerPlayerEntity p) {
        CrewState s = Game.S();
        if (s.story != LEDGER) return;
        takeLedger();
        say("pell", "Well, well. The Baroness was going to frame YOU for all of this.",
                "Consider your record... misplaced. Here's the reward. Don't make me regret this.");
        Sched.later(sayDelay(2), () -> {
            CrewState st = Game.S();
            st.heat = 0;
            st.pot += 300;
            st.addRep("concordat", 40);
            st.addRep("syndicate", -50);
            st.ending = "clean";
            st.markDirty();
            Game.crew("Heat wiped to 0! +300 credits reward in the Crew Pot.", Formatting.GREEN);
            finale("Clean Slate");
        });
    }
}
