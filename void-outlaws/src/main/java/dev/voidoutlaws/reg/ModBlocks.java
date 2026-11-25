package dev.voidoutlaws.reg;

import dev.voidoutlaws.VoidOutlaws;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlocks {
    private ModBlocks() {}

    public static final Map<OutlawBlock.Kind, Block> BY_KIND = new EnumMap<>(OutlawBlock.Kind.class);

    public static void init() {
        for (OutlawBlock.Kind k : OutlawBlock.Kind.values()) {
            AbstractBlock.Settings s = AbstractBlock.Settings.create()
                    .mapColor(MapColor.IRON_GRAY)
                    .strength(-1.0f, 3600000.0f)
                    .dropsNothing()
                    .sounds(BlockSoundGroup.METAL)
                    .luminance(st -> k.light);
            Block b = Registry.register(Registries.BLOCK, VoidOutlaws.id(k.id), new OutlawBlock(k, s));
            Registry.register(Registries.ITEM, VoidOutlaws.id(k.id), new BlockItem(b, new Item.Settings()));
            BY_KIND.put(k, b);
        }
    }

    public static Block get(OutlawBlock.Kind k) {
        return BY_KIND.get(k);
    }
}
