package dev.voidoutlaws.game;

import dev.voidoutlaws.menu.*;
import dev.voidoutlaws.reg.OutlawBlock;
import dev.voidoutlaws.state.CrewState;
import dev.voidoutlaws.world.Loc;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/** What happens when a player right-clicks one of our blocks. */
public final class Interact {
    private Interact() {}

    public static void useBlock(OutlawBlock.Kind kind, ServerPlayerEntity p, BlockPos pos) {
        if (Game.server == null) return;
        CrewState s = Game.S();
        Loc here = Game.locOf(p);
        switch (kind) {
            case SHIP_CONSOLE -> {
                if (!Roles.claim(p, Roles.Role.PILOT)) return;
                Menu.open(p, new ConsoleView());
                Tutorial.onConsoleOpen(p);
            }
            case GUNNERY_STATION -> { if (Roles.claim(p, Roles.Role.GUNNER)) Menu.open(p, new GunneryView()); }
            case ENGINEERING_PANEL -> { if (Roles.claim(p, Roles.Role.MECHANIC)) Menu.open(p, new EngineView()); }
            case CARGO_HOLD -> p.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (id, inv, pl) -> GenericContainerScreenHandler.createGeneric9x3(id, inv, s.cargo),
                    Text.literal("Cargo Hold (scans search this!)")));
            case HIDDEN_COMPARTMENT -> {
                Game.sound(p, SoundEvents.BLOCK_IRON_DOOR_OPEN, 0.6f, 1.6f);
                p.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                        (id, inv, pl) -> new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X1, id, inv, s.hidden, 1) {
                            @Override
                            public void onClosed(PlayerEntity player) {
                                super.onClosed(player);
                                if (player instanceof ServerPlayerEntity sp) Tutorial.onHiddenClosed(sp);
                            }
                        },
                        Text.literal("Hidden Compartment (shh!)")));
            }
            case SHIP_HATCH -> {
                if (s.jumping) {
                    Game.msg(p, "We're in hyperspace! Stepping outside right now would be... a bad idea.", Formatting.YELLOW);
                    return;
                }
                Tp.toLoc(p, s.shipLoc);
                Game.bar(p, "Welcome to " + s.shipLoc.display + "!", s.shipLoc.color);
                Tutorial.onHatch(p);
            }
            case DOCKING_BEACON -> {
                if (s.jumping) {
                    Game.msg(p, "The ship is in hyperspace. Wait until it docks, then use the beacon again.", Formatting.YELLOW);
                    return;
                }
                if (here == s.shipLoc) {
                    Tp.toShip(p);
                    Game.bar(p, "Welcome back aboard!", Formatting.AQUA);
                } else {
                    Game.msg(p, "Calling a shuttle to the ship at " + s.shipLoc.display + "... hold still!", Formatting.AQUA);
                    Game.sound(p, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1f, 1f);
                    Sched.later(60, () -> {
                        if (!p.isRemoved() && !Game.S().jumping) Tp.toShip(p);
                    });
                }
            }
            case JOB_BOARD -> Menu.open(p, new JobBoardView(here == null ? Loc.DUSTREACH : here, true));
            case DEAD_DROP -> Menu.open(p, new JobBoardView(here == null ? Loc.MERIDIAN : here, false));
            case BLACK_MARKET -> Menu.open(p, new ShopView(here == null ? Loc.DUSTREACH : here));
            case CARD_TABLE -> Menu.open(p, new CardsView());
            case VAULT_TERMINAL -> Missions.vaultTerminal(p);
            case LOOT_CRATE -> Missions.crate(p, pos);
            case LOOT_LOCKER -> Menu.open(p, new LockerView());
            case FINE_KIOSK -> Menu.open(p, new KioskView());
            case BULKHEAD_PANEL -> Missions.bulkhead(p);
        }
    }
}
