package dev.forgottenisland.game;

import dev.forgottenisland.content.Content;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Instruments;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Named items built on vanilla mechanics. Every item made here carries custom_data {fi_item:"id"}
 * so quests can recognise it. Items flagged "quest" are restored automatically if lost.
 */
public final class KeyItems {
    private KeyItems() {}

    public record Def(String id, Item item, int count, String name, Formatting color, boolean quest, String[] lore, BiConsumer<ItemStack, MinecraftServer> extra) {}
    private static final Map<String, Def> DEFS = new LinkedHashMap<>();

    private static void def(String id, Item item, int count, String name, Formatting color, boolean quest, BiConsumer<ItemStack, MinecraftServer> extra, String... lore) {
        DEFS.put(id, new Def(id, item, count, name, color, quest, lore, extra));
    }

    private static BiConsumer<ItemStack, MinecraftServer> ench(Object... pairs) {
        return (st, server) -> {
            var reg = server.getRegistryManager().getWrapperOrThrow(RegistryKeys.ENCHANTMENT);
            for (int i = 0; i < pairs.length; i += 2) {
                @SuppressWarnings("unchecked") RegistryKey<Enchantment> key = (RegistryKey<Enchantment>) pairs[i];
                st.addEnchantment(reg.getOrThrow(key), (Integer) pairs[i + 1]);
            }
        };
    }
    private static BiConsumer<ItemStack, MinecraftServer> book(String bookId) {
        return (st, server) -> {
            Content.Book b = ContentHolder.get().books.get(bookId);
            if (b == null) return;
            List<RawFilteredPair<Text>> pages = new ArrayList<>();
            for (String p : b.pages) pages.add(RawFilteredPair.of(Text.literal(p)));
            st.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, new WrittenBookContentComponent(RawFilteredPair.of(b.title), b.author, 0, pages, true));
        };
    }

    static {
        def("journal", ModItems.JOURNAL, 1, "Traveller's Journal", Formatting.GOLD, true, null, "Objectives, people and hints.", "Right-click or press J.");
        def("letter", Items.WRITTEN_BOOK, 1, "A Salt-Stained Letter", Formatting.AQUA, true, book("letter"), "It found you, somehow.");
        def("ember", ModItems.EMBER, 1, "The Lantern Ember", Formatting.LIGHT_PURPLE, true, null, "Warm as a held hand.");
        def("warden_key", Items.TRIPWIRE_HOOK, 1, "Warden's Key", Formatting.YELLOW, true, null, "Iron, cold, stamped with a kestrel.");
        def("bread", Items.BREAD, 6, null, null, false, null);
        def("arrows", Items.ARROW, 24, null, null, false, null);
        def("smith_sword", Items.IRON_SWORD, 1, "Corin's Spare Blade", Formatting.WHITE, false, null, "Plain, honest work.");
        def("kestrel_blade", Items.IRON_SWORD, 1, "Kestrel Blade", Formatting.GOLD, false, ench(Enchantments.SHARPNESS, 3, Enchantments.UNBREAKING, 3, Enchantments.LOOTING, 1), "Forged from the Keep's own steel.");
        def("spyglass", Items.SPYGLASS, 1, "Harbormaster's Spyglass", Formatting.AQUA, false, null, "Brass worn smooth by one thumb.");
        def("wendel_charm", Items.TOTEM_OF_UNDYING, 1, "Wendel's Charm", Formatting.GOLD, false, null, "Carved by Maud Pike, long ago.", "It has kept one fisherman alive.");
        def("owl_whistle", Items.GOAT_HORN, 1, "Owl Whistle", Formatting.GREEN, false, (st, s) -> st.set(DataComponentTypes.INSTRUMENT, Registries.INSTRUMENT.entryOf(Instruments.PONDER_GOAT_HORN)), "A carved whistle, shaped like a little owl.");
        def("diver_draught", Items.POTION, 1, "Diver's Draught", Formatting.AQUA, false, (st, s) -> st.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.LONG_WATER_BREATHING)), "Hester's own brew. Tastes of kelp and mint.");
        def("tinderbox", Items.FLINT_AND_STEEL, 1, "Wendel's Tinderbox", Formatting.YELLOW, true, null, "For the old waystone lamps.");
        def("wren_bow", Items.BOW, 1, "Wren's Bow", Formatting.GREEN, false, ench(Enchantments.POWER, 2, Enchantments.UNBREAKING, 2), "Yew, waxed and lovingly kept.");
        def("mother_bow", Items.BOW, 1, "Ashdown Longbow", Formatting.GREEN, true, null, "Old yew, carved with owls.");
        def("logbook", Items.WRITTEN_BOOK, 1, "Logbook of the Meridian", Formatting.AQUA, true, book("logbook"), "Waterlogged, but legible.");
        def("maud_diary", Items.WRITTEN_BOOK, 1, "Maud Pike's Diary", Formatting.AQUA, false, book("maud_diary"));
        def("warden_journal", Items.WRITTEN_BOOK, 1, "The Warden's Log", Formatting.AQUA, false, book("warden_log"));
        def("page_1", Items.PAPER, 1, "Chronicle Page (I)", Formatting.YELLOW, true, null, "A torn page in faded ink.");
        def("page_2", Items.PAPER, 1, "Chronicle Page (II)", Formatting.YELLOW, true, null, "A torn page in faded ink.");
        def("page_3", Items.PAPER, 1, "Chronicle Page (III)", Formatting.YELLOW, true, null, "A torn page in faded ink.");
        def("brass_compass", Items.COMPASS, 1, "Brass Ship's Compass", Formatting.YELLOW, true, null, "Its needle still swings true.");
        def("sailcloth", Items.WHITE_WOOL, 1, "Bolt of Sailcloth", Formatting.YELLOW, true, null, "Heavy canvas, stored dry for decades.");
        def("mast_timber", Items.STRIPPED_SPRUCE_LOG, 1, "Heartwood Spar", Formatting.YELLOW, true, null, "Straight, dense, perfect for a mast.");
        def("keep_steel", Items.IRON_INGOT, 3, "Kestrel Steel", Formatting.YELLOW, true, null, "Old ingots stamped with a kestrel.");
        def("sea_lily", Items.LILY_OF_THE_VALLEY, 1, "Sea-Lily", Formatting.LIGHT_PURPLE, false, null, "A rare flower that grows only on Gull Rock.");
        def("star_chart", Items.GLOBE_BANNER_PATTERN, 1, "Old Star Chart", Formatting.LIGHT_PURPLE, false, null, "Hand-inked constellations and sea lanes.");
        def("locket", Items.HEART_OF_THE_SEA, 1, "Silver Gull Locket", Formatting.LIGHT_PURPLE, true, null, "Tiny, tarnished, and warm.");
        def("ancient_coin", Items.GOLD_NUGGET, 1, "Lanternwright Coin", Formatting.GOLD, false, null, "Stamped with a flame inside an eye.");
        def("warden_shield", Items.SHIELD, 1, "Warden's Shield", Formatting.GOLD, false, ench(Enchantments.UNBREAKING, 3), "The Keep's last standing guard.");
        def("diver_helm", Items.TURTLE_HELMET, 1, "Diver's Helm", Formatting.AQUA, false, ench(Enchantments.RESPIRATION, 3, Enchantments.AQUA_AFFINITY, 1));
        def("ranger_bow", Items.BOW, 1, "Old Ranger's Bow", Formatting.GREEN, false, ench(Enchantments.POWER, 3, Enchantments.INFINITY, 1), "Left behind the falls, for whoever was brave enough.");
        def("moss_crown", Items.CARVED_PUMPKIN, 1, "Crown of the Mosswood", Formatting.GREEN, false, null, "A joke from a forgotten festival.");
        def("cartographer_compass", Items.RECOVERY_COMPASS, 1, "Cartographer's Compass", Formatting.AQUA, false, null, "It always points to where you last fell.");
        def("keep_mail", Items.CHAINMAIL_CHESTPLATE, 1, "Kestrel Guard Hauberk", Formatting.WHITE, false, ench(Enchantments.PROTECTION, 1));
        def("keep_helm", Items.CHAINMAIL_HELMET, 1, "Kestrel Guard Helm", Formatting.WHITE, false, null);
        def("keep_legs", Items.CHAINMAIL_LEGGINGS, 1, "Kestrel Guard Greaves", Formatting.WHITE, false, null);
        def("keep_boots", Items.CHAINMAIL_BOOTS, 1, "Kestrel Guard Boots", Formatting.WHITE, false, null);
        def("healing", Items.POTION, 2, "Hester's Tonic", Formatting.RED, false, (st, s) -> st.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.STRONG_HEALING)));
        def("golden_apple", Items.GOLDEN_APPLE, 1, null, null, false, null);
        def("emeralds", Items.EMERALD, 5, null, null, false, null);
        def("lore_book_inn", Items.WRITTEN_BOOK, 1, null, null, false, book("inn_cellar"));
        def("lore_book_miller", Items.WRITTEN_BOOK, 1, null, null, false, book("miller"));
        def("lore_book_kings", Items.WRITTEN_BOOK, 1, null, null, false, book("kings_mural"));
        def("lullaby", Items.WRITTEN_BOOK, 1, null, null, false, book("lullaby"));
    }

    public static Set<String> ids() { return DEFS.keySet(); }
    public static Def def(String id) { return DEFS.get(id); }
    public static boolean isQuest(String id) { Def d = DEFS.get(id); return d != null && d.quest(); }

    public static ItemStack create(String id, MinecraftServer server) { return create(id, -1, server); }

    public static ItemStack create(String id, int count, MinecraftServer server) {
        Def d = DEFS.get(id);
        if (d == null) throw new IllegalArgumentException("Unknown key item " + id);
        ItemStack st = new ItemStack(d.item(), count > 0 ? count : d.count());
        NbtCompound tag = new NbtCompound();
        tag.putString("fi_item", id);
        st.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
        if (d.name() != null) st.set(DataComponentTypes.CUSTOM_NAME, Text.literal(d.name()).formatted(d.color() == null ? Formatting.WHITE : d.color()).styled(s -> s.withItalic(false)));
        if (d.lore().length > 0) {
            List<Text> lines = new ArrayList<>();
            for (String l : d.lore()) lines.add(Text.literal(l).formatted(Formatting.GRAY, Formatting.ITALIC));
            st.set(DataComponentTypes.LORE, new LoreComponent(lines));
        }
        if (d.quest()) st.set(DataComponentTypes.RARITY, Rarity.UNCOMMON);
        if (d.extra() != null) d.extra().accept(st, server);
        return st;
    }

    public static String idOf(ItemStack st) {
        NbtComponent c = st.get(DataComponentTypes.CUSTOM_DATA);
        if (c == null) return null;
        NbtCompound n = c.copyNbt();
        return n.contains("fi_item") ? n.getString("fi_item") : null;
    }

    public static int count(PlayerEntity p, String id) {
        int n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) { ItemStack st = inv.getStack(i); if (!st.isEmpty() && id.equals(idOf(st))) n += st.getCount(); }
        return n;
    }

    public static void give(ServerPlayerEntity p, String id, int count) {
        ItemStack st = create(id, count, p.getServer());
        if (!p.getInventory().insertStack(st)) p.dropItem(st, false);
        if (isQuest(id)) dev.forgottenisland.game.Game.state(p.getServer()).set("got." + id);
    }

    public static boolean take(ServerPlayerEntity p, String id, int count) {
        var inv = p.getInventory();
        int need = count;
        for (int i = 0; i < inv.size() && need > 0; i++) {
            ItemStack st = inv.getStack(i);
            if (!st.isEmpty() && id.equals(idOf(st))) { int k = Math.min(need, st.getCount()); st.decrement(k); need -= k; }
        }
        if (isQuest(id)) dev.forgottenisland.game.Game.state(p.getServer()).set("used." + id);
        return need == 0;
    }
}
