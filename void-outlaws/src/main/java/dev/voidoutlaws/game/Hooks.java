package dev.voidoutlaws.game;

import dev.voidoutlaws.menu.HackView;
import dev.voidoutlaws.menu.Menu;
import dev.voidoutlaws.menu.PowerView;
import dev.voidoutlaws.state.CrewState;
import dev.voidoutlaws.world.Build;
import dev.voidoutlaws.world.Loc;
import dev.voidoutlaws.world.Structures;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

/** Wires all Fabric events to the game. */
public final class Hooks {
    private Hooks() {}

    public static void init() {
        Structures.dryRun();

        ServerLifecycleEvents.SERVER_STARTED.register(Game::onStart);
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> Game.onStop());
        ServerTickEvents.END_SERVER_TICK.register(Game::tick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.player;
            Sched.later(20, () -> onJoin(p));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity p = handler.player;
            Roles.releaseAll(p);
            if (Game.server != null) {
                for (ServerPlayerEntity o : Game.players()) {
                    if (o != p) Game.msg(o, Game.name(p) + " left the crew for now. The ship keeps their seat warm.", Formatting.GRAY);
                }
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> {
            if (alive || Game.server == null) return;
            Sched.later(2, () -> {
                if (newP.isRemoved()) return;
                CrewState s = Game.S();
                int bill = Math.min(40, s.credits(newP.getUuid()) / 10);
                s.addCredits(newP.getUuid(), -bill);
                Tp.toShip(newP);
                Game.msg(newP, "You wake up in the ship's med bay. " + (bill > 0 ? "The med-bot charged you " + bill + " credits. It also stole your lollipop." : "The med-bot gives you a lollipop."), Formatting.AQUA);
            });
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!(entity instanceof VillagerEntity)) return ActionResult.PASS;
            Npcs.Def d = Npcs.of(entity);
            if (d == null) return ActionResult.PASS;
            if (hit == null && hand == Hand.MAIN_HAND) Story.talk(sp, d.id());
            return ActionResult.SUCCESS;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
            if (!(world instanceof ServerWorld sw) || player.isCreative() || Game.server == null) return true;
            if (!Loc.isModWorld(sw.getRegistryKey())) return true;
            if (Structures.isProtected(Game.S(), sw.getRegistryKey(), pos) && Build.PALETTE.contains(state.getBlock())) {
                if (player instanceof ServerPlayerEntity sp) Game.bar(sp, "That's part of the scenery. Hands off!", Formatting.GRAY);
                return false;
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (Game.server != null && !entity.getWorld().isClient) Mobs.onDeath(entity);
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity.getCommandTags().contains("vo_companion")) return false;
            for (String t : entity.getCommandTags()) if (t.startsWith("vo_npc_")) return false;
            if (entity instanceof ServerPlayerEntity sp && source.getAttacker() != null && source.getAttacker() != sp
                    && sp.currentScreenHandler instanceof Menu m && (m.view instanceof HackView || m.view instanceof PowerView)) {
                Sched.later(1, () -> {
                    sp.closeHandledScreen();
                    Game.msg(sp, "You got hit! The hack was interrupted. Get someone to cover you, then try again.", Formatting.RED);
                });
            }
            return true;
        });
    }

    private static void onJoin(ServerPlayerEntity p) {
        if (p.isRemoved() || Game.server == null) return;
        CrewState s = Game.S();
        boolean fresh = s.known.add(p.getUuid());
        if (fresh) {
            s.credits.putIfAbsent(p.getUuid(), 100);
            s.markDirty();
            for (ServerPlayerEntity o : Game.players()) {
                if (o != p) Game.msg(o, Game.name(p) + " joined the crew of the " + Game.SHIP_NAME + "! Say hi!", Formatting.GREEN);
            }
            Tp.toShip(p);
            Game.msg(p, "Welcome to Void Outlaws! You start with 100 credits. Your tutorial starts now.", Formatting.GOLD);
            Tutorial.start(p);
        } else {
            if (!Loc.isModWorld(p.getServerWorld().getRegistryKey())) Tp.toShip(p);
            Game.msg(p, "Welcome back! The ship is " + (s.jumping ? "jumping to " + s.jumpDest.display : "docked at " + s.shipLoc.display)
                    + ". Heat: " + s.heat + ". Type /guide for a reminder.", Formatting.AQUA);
            if (Tutorial.step(p) >= 0) Tutorial.showCurrent(p);
            for (ServerPlayerEntity o : Game.players()) {
                if (o != p) Game.msg(o, Game.name(p) + " is back aboard!", Formatting.GREEN);
            }
        }
        Story.onJoin(p);
    }
}
