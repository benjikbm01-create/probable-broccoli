package dev.voidoutlaws.menu;

import dev.voidoutlaws.game.JumpEvent;
import dev.voidoutlaws.game.Jumps;
import dev.voidoutlaws.game.Roles;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Gunner station: click enemy ships before they fire. */
public class GunneryView extends View {
    @Override public Text title() { return Text.literal("Gunnery Station (Gunner)"); }
    @Override public int rows() { return 5; }
    @Override public Roles.Role role() { return Roles.Role.GUNNER; }

    @Override
    public void render(ServerPlayerEntity p, Slots g) {
        g.fill(Btn.pane(Items.BLACK_STAINED_GLASS_PANE));
        JumpEvent e = Jumps.event;
        if (e == null || !e.usesTargets()) {
            g.set(4, Btn.of(Items.CROSSBOW, "Gunnery Station", Formatting.RED, "Scopes are clear."));
            g.set(22, Btn.of(Items.SPYGLASS, "No targets", Formatting.GRAY,
                    "Enemy ships show up here during", "pirate attacks and police chases.",
                    "Click them before they fire!"));
            return;
        }
        boolean pirates = e.type == JumpEvent.Type.PIRATES;
        for (int i = 0; i < 45; i++) {
            int t = e.targets[i];
            if (t <= 0 || i == 4) continue;
            if (pirates) {
                boolean urgent = t < 15;
                g.set(i, Btn.of(urgent ? Items.ORANGE_STAINED_GLASS : Items.RED_STAINED_GLASS,
                        urgent ? "PIRATE FIGHTER - ABOUT TO FIRE!" : "Pirate fighter! CLICK IT!", Formatting.RED));
            } else {
                g.set(i, Btn.of(Items.LIGHT_BLUE_STAINED_GLASS, "Tracking drone! CLICK IT!", Formatting.AQUA));
            }
        }
        String head = pirates ? "Enemies left: " + Math.max(0, e.squadronHp) : "Drones shot: " + e.hits;
        g.set(4, Btn.of(Items.CROSSBOW, head, Formatting.RED,
                pirates ? "Click RED ships before they shoot us!" : "Click BLUE drones to help us escape!",
                e.flashTicks > 0 ? "§e" + e.flash : " ", e.secondsLeft() + "s left"));
    }

    @Override
    public void click(ServerPlayerEntity p, int slot, boolean right) {
        if (slot != 4) Jumps.gunnerClick(p, slot);
    }

    @Override
    public void closed(ServerPlayerEntity p) {
        Roles.release(p, Roles.Role.GUNNER);
    }
}
