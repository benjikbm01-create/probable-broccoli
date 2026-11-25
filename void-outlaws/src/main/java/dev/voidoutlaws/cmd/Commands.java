package dev.voidoutlaws.cmd;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.voidoutlaws.game.*;
import dev.voidoutlaws.reg.ModItems;
import dev.voidoutlaws.state.CrewState;
import dev.voidoutlaws.world.Loc;
import dev.voidoutlaws.world.Structures;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class Commands {
    private Commands() {}

    private static final Map<UUID, Long> UNSTUCK_CD = new HashMap<>();

    public static void init() {
        CommandRegistrationCallback.EVENT.register((d, reg, env) -> register(d));
    }

    private static int ok() { return 1; }

    private static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(literal("tutorial")
                .executes(c -> { Tutorial.showCurrent(c.getSource().getPlayerOrThrow()); return ok(); })
                .then(literal("restart").executes(c -> { Tutorial.start(c.getSource().getPlayerOrThrow()); return ok(); }))
                .then(literal("skip").executes(c -> { Tutorial.skip(c.getSource().getPlayerOrThrow()); return ok(); }))
                .then(literal("hints")
                        .then(literal("on").executes(c -> hints(c, true)))
                        .then(literal("off").executes(c -> hints(c, false)))));

        d.register(literal("guide").executes(c -> { guide(c.getSource().getPlayerOrThrow()); return ok(); }));
        d.register(literal("crew").executes(c -> { crew(c.getSource().getPlayerOrThrow()); return ok(); }));

        d.register(literal("pay")
                .then(argument("player", EntityArgumentType.player())
                        .then(argument("amount", IntegerArgumentType.integer(1))
                                .executes(c -> {
                                    ServerPlayerEntity from = c.getSource().getPlayerOrThrow();
                                    ServerPlayerEntity to = EntityArgumentType.getPlayer(c, "player");
                                    int amt = IntegerArgumentType.getInteger(c, "amount");
                                    CrewState s = Game.S();
                                    if (to == from) { Game.msg(from, "You paid yourself. The economy is thrilled.", Formatting.GRAY); return ok(); }
                                    if (s.credits(from.getUuid()) < amt) { Game.msg(from, "You only have " + s.credits(from.getUuid()) + " cr.", Formatting.RED); return 0; }
                                    s.addCredits(from.getUuid(), -amt);
                                    s.addCredits(to.getUuid(), amt);
                                    Game.msg(from, "You paid " + Game.name(to) + " " + amt + " cr.", Formatting.GOLD);
                                    Game.msg(to, Game.name(from) + " paid you " + amt + " cr. How generous!", Formatting.GOLD);
                                    return ok();
                                }))));

        d.register(literal("unstuck").executes(c -> {
            ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
            long now = Game.tick;
            Long last = UNSTUCK_CD.get(p.getUuid());
            if (last != null && now - last < 600) { Game.msg(p, "Wait a bit before using /unstuck again.", Formatting.YELLOW); return 0; }
            UNSTUCK_CD.put(p.getUuid(), now);
            Tp.toShip(p);
            Game.msg(p, "Beamed back aboard the ship.", Formatting.AQUA);
            return ok();
        }));

        d.register(literal("outlaws").requires(src -> src.hasPermissionLevel(2))
                .then(literal("heat").then(argument("value", IntegerArgumentType.integer(0, 100)).executes(c -> {
                    CrewState s = Game.S(); s.heat = IntegerArgumentType.getInteger(c, "value"); s.markDirty();
                    return fb(c, "Heat set to " + s.heat);
                })))
                .then(literal("hull").then(argument("value", IntegerArgumentType.integer(0, 100)).executes(c -> {
                    CrewState s = Game.S(); s.hull = IntegerArgumentType.getInteger(c, "value"); s.markDirty();
                    return fb(c, "Hull set to " + s.hull);
                })))
                .then(literal("pot").then(argument("value", IntegerArgumentType.integer(0)).executes(c -> {
                    CrewState s = Game.S(); s.pot = IntegerArgumentType.getInteger(c, "value"); s.markDirty();
                    return fb(c, "Crew pot set to " + s.pot);
                })))
                .then(literal("credits").then(argument("player", EntityArgumentType.player()).then(argument("value", IntegerArgumentType.integer(0)).executes(c -> {
                    ServerPlayerEntity p = EntityArgumentType.getPlayer(c, "player");
                    CrewState s = Game.S(); s.credits.put(p.getUuid(), IntegerArgumentType.getInteger(c, "value")); s.markDirty();
                    return fb(c, "Credits set");
                }))))
                .then(literal("story").then(argument("stage", IntegerArgumentType.integer(0, 8)).executes(c -> {
                    CrewState s = Game.S(); s.story = IntegerArgumentType.getInteger(c, "stage"); s.storyCounter = 0; s.markDirty();
                    return fb(c, "Story stage set to " + s.story);
                })))
                .then(literal("rebuild").executes(c -> {
                    CrewState s = Game.S();
                    Structures.rebuildShip(Game.server, s);
                    for (Loc l : Loc.values()) Structures.rebuild(Game.server, s, l);
                    return fb(c, "All locations rebuilt");
                }))
                .then(literal("canceljob").executes(c -> {
                    CrewState s = Game.S(); s.job = null; s.markDirty();
                    return fb(c, "Job cleared");
                })));
    }

    private static int fb(CommandContext<ServerCommandSource> c, String msg) {
        c.getSource().sendFeedback(() -> Text.literal(msg).formatted(Formatting.YELLOW), true);
        return ok();
    }

    private static int hints(CommandContext<ServerCommandSource> c, boolean on) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity p = c.getSource().getPlayerOrThrow();
        CrewState s = Game.S();
        if (on) s.hintsOff.remove(p.getUuid()); else s.hintsOff.add(p.getUuid());
        s.markDirty();
        Game.msg(p, "Tutorial reminders " + (on ? "ON" : "OFF") + ".", Formatting.GRAY);
        return ok();
    }

    private static void line(ServerPlayerEntity p, String head, String body) {
        p.sendMessage(Text.literal(head).formatted(Formatting.GOLD).append(Text.literal(body).formatted(Formatting.WHITE)));
    }

    public static void guide(ServerPlayerEntity p) {
        p.sendMessage(Text.literal("===== Void Outlaws: Quick Guide =====").formatted(Formatting.GOLD, Formatting.BOLD));
        line(p, "Ship Console ", "(front of ship): star map + PILOT. Click a place to jump.");
        line(p, "Gunnery Station ", "GUNNER: click red ships / blue drones during attacks.");
        line(p, "Engineering Panel ", "MECHANIC: click sparks, boost, swap transponder.");
        line(p, "Hatch ", "(cargo bay): step outside. ");
        line(p, "Docking Beacon ", "(every landing pad): back to the ship.");
        line(p, "Hidden Compartment ", "(cargo bay floor): scans almost never find it.");
        line(p, "Cargo Hold ", "storage that DOES get scanned.");
        line(p, "Job Boards ", "take & deliver jobs. Loot Locker: split the Crew Pot.");
        line(p, "Heat ", "= wanted level. Lower it at The Burrow, the Meridian kiosk, bribes, or a Transponder Chip.");
        line(p, "Scans ", "pilot picks COMPLY, BRIBE or RUN on the Ship Console.");
        line(p, "Commands ", "/crew  /pay <name> <amount>  /tutorial  /unstuck");
        if (!Game.hasItem(p, ModItems.HANDBOOK)) {
            p.getInventory().offerOrDrop(Books.handbook());
            Game.msg(p, "Here's a fresh Outlaw's Handbook (right-click to read).", Formatting.GRAY);
        }
    }

    public static void crew(ServerPlayerEntity p) {
        CrewState s = Game.S();
        p.sendMessage(Text.literal("===== Crew of the " + Game.SHIP_NAME + " =====").formatted(Formatting.GOLD, Formatting.BOLD));
        line(p, "Ship: ", s.jumping ? "jumping to " + s.jumpDest.display : "docked at " + s.shipLoc.display);
        line(p, "Heat: ", s.heat + " (" + Heat.level(s.heat) + ")   Hull: " + s.hull + "%   Transponder: " + s.transponder);
        line(p, "Crew Pot: ", s.pot + " cr   Your wallet: " + s.credits(p.getUuid()) + " cr");
        line(p, "Job: ", s.job == null ? "none (visit a Job Board)" : s.job.title + " - " + s.job.shortLine());
        line(p, "Goal: ", Story.goal());
        line(p, "Reputation: ", "Syndicate " + s.rep("syndicate") + ", Free Folk " + s.rep("freefolk")
                + ", Concordat " + s.rep("concordat") + ", Rattlejaw " + s.rep("rattlejaw"));
        StringBuilder roles = new StringBuilder();
        for (Roles.Role r : Roles.Role.values()) {
            ServerPlayerEntity h = Roles.holder(r);
            roles.append(r.display).append(": ").append(h == null ? "-" : Game.name(h)).append("  ");
        }
        line(p, "Stations: ", roles.toString());
        StringBuilder online = new StringBuilder();
        for (ServerPlayerEntity o : Game.players()) online.append(Game.name(o)).append(Game.isAboard(o) ? " (aboard) " : " ");
        line(p, "Online: ", online.toString());
    }
}
