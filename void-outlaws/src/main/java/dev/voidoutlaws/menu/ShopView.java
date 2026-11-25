package dev.voidoutlaws.menu;

import dev.voidoutlaws.game.Game;
import dev.voidoutlaws.reg.ModItems;
import dev.voidoutlaws.state.CrewState;
import dev.voidoutlaws.world.Loc;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Black market: buy gear, buy/sell contraband (prices differ between places). */
public class ShopView extends View {
    record Offer(Item item, int count, int price, String desc) {}

    private final Loc here;
    private final List<Offer> offers = new ArrayList<>();

    public ShopView(Loc here) {
        this.here = here;
        offers.add(new Offer(ModItems.BLASTER, 1, 120, "Pew pew. Stuns crewmates, hurts enemies."));
        offers.add(new Offer(ModItems.JETPACK, 1, 180, "Short hops. Very short. Very fun."));
        offers.add(new Offer(ModItems.GRAPPLING_HOOK, 1, 140, "Zip to any block up to 32 away."));
        offers.add(new Offer(ModItems.DISGUISE_KIT, 1, 60, "5 minutes of being someone else."));
        offers.add(new Offer(ModItems.TRANSPONDER_CHIP, 1, 150, "New ship ID = Heat -35 (Engineering Panel)."));
        offers.add(new Offer(ModItems.HULL_PATCH, 1, 40, "+30% hull. Smells like glue."));
        offers.add(new Offer(ModItems.NEBULA_NOODLES, 2, 8, "Tasty space food."));
        offers.add(new Offer(ModItems.SPACE_TOMATO, 4, 5, "For throwing. Not for eating."));
        offers.add(new Offer(ModItems.COMPANION_WHISTLE, 1, 200, "Adopt a little alien buddy!"));
        if (here == Loc.GILDED_MAW) {
            offers.add(new Offer(ModItems.STOLEN_TECH, 1, 45, "Sells for more on Dustreach."));
            offers.add(new Offer(ModItems.HUM_CRYSTAL, 1, 35, "Sells for more on Dustreach."));
            offers.add(new Offer(ModItems.GRUMBLEBACK_EGG, 1, 60, "Handle with care. It bites."));
        } else {
            offers.add(new Offer(ModItems.VOID_DUST, 1, 25, "Sells for more on the Gilded Maw."));
            offers.add(new Offer(ModItems.GLOWMOSS, 1, 20, "Sells for more on the Gilded Maw."));
        }
    }

    @Override public Text title() { return Text.literal("Black Market - " + here.display); }
    @Override public int rows() { return 4; }

    private String faction() {
        return here == Loc.GILDED_MAW ? "syndicate" : "freefolk";
    }

    private int price(int base) {
        int rep = Math.max(-50, Math.min(100, Game.S().rep(faction())));
        return Math.max(1, (int) Math.round(base * (1.0 - rep / 400.0)));
    }

    public int sellPrice(Item it) {
        boolean maw = here == Loc.GILDED_MAW;
        if (it == ModItems.VOID_DUST) return maw ? 38 : 15;
        if (it == ModItems.GLOWMOSS) return maw ? 30 : 12;
        if (it == ModItems.STOLEN_TECH) return maw ? 27 : 60;
        if (it == ModItems.HUM_CRYSTAL) return maw ? 21 : 48;
        if (it == ModItems.GRUMBLEBACK_EGG) return maw ? 36 : 70;
        return 0;
    }

    @Override
    public void render(ServerPlayerEntity p, Slots g) {
        CrewState s = Game.S();
        g.fill(Btn.pane(Items.PURPLE_STAINED_GLASS_PANE));
        for (int i = 0; i < offers.size() && i < 27; i++) {
            Offer o = offers.get(i);
            ItemStack st = Btn.of(o.item, o.item.getName().getString() + (o.count > 1 ? " x" + o.count : ""), Formatting.YELLOW,
                    o.desc, "§6Price: " + price(o.price) + " cr", "Click to buy");
            g.set(i, Btn.count(st, o.count));
        }
        g.set(27, Btn.of(Items.GOLD_NUGGET, "Your wallet: " + s.credits(p.getUuid()) + " cr", Formatting.GOLD,
                "Discount from " + (here == Loc.GILDED_MAW ? "Syndicate" : "Free Folk") + " rep: " + s.rep(faction())));
        List<String> lore = new ArrayList<>();
        lore.add("Sells ALL contraband in your pockets.");
        lore.add("(Not job cargo... unless you want to.)");
        for (Item it : ModItems.CONTRABAND) lore.add(it.getName().getString() + ": " + sellPrice(it) + " cr each");
        g.set(31, Btn.of(Items.HOPPER, "SELL contraband", Formatting.GREEN, lore.toArray(new String[0])));
        g.set(35, Btn.close());
    }

    @Override
    public void click(ServerPlayerEntity p, int slot, boolean right) {
        CrewState s = Game.S();
        if (slot == 35) { p.closeHandledScreen(); return; }
        if (slot == 31) {
            PlayerInventory inv = p.getInventory();
            int total = 0, n = 0;
            for (int i = 0; i < inv.size(); i++) {
                ItemStack st = inv.getStack(i);
                if (!ModItems.isContraband(st)) continue;
                total += sellPrice(st.getItem()) * st.getCount();
                n += st.getCount();
                inv.setStack(i, ItemStack.EMPTY);
            }
            if (n == 0) { Game.bar(p, "You have no contraband in your pockets.", Formatting.GRAY); return; }
            s.addCredits(p.getUuid(), total);
            Game.msg(p, "Sold " + n + " contraband for " + total + " cr. Wallet: " + s.credits(p.getUuid()) + " cr.", Formatting.GOLD);
            Game.sound(p, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            return;
        }
        if (slot >= 0 && slot < offers.size()) {
            Offer o = offers.get(slot);
            int cost = price(o.price);
            if (s.credits(p.getUuid()) < cost) { Game.bar(p, "Not enough credits! (" + cost + " cr)", Formatting.RED); return; }
            s.addCredits(p.getUuid(), -cost);
            p.getInventory().offerOrDrop(new ItemStack(o.item, o.count));
            Game.sound(p, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.4f);
            Game.bar(p, "Bought " + o.item.getName().getString() + " for " + cost + " cr.", Formatting.GREEN);
        }
    }
}
