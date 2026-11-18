package dev.forgottenisland.game;

import dev.forgottenisland.ForgottenIsland;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/** Puzzle blocks (unbreakable; interaction handled in Puzzles). */
public final class ModBlocks {
    private ModBlocks() {}

    /** A polished mirror on a pedestal. FLIP=false reflects like '/', true like '\' (seen from above, north up). */
    public static class MirrorBlock extends Block {
        public static final BooleanProperty FLIP = BooleanProperty.of("flip");
        private static final VoxelShape SHAPE = Block.createCuboidShape(1, 0, 1, 15, 15, 15);
        public MirrorBlock(Settings s) { super(s); setDefaultState(getDefaultState().with(FLIP, false)); }
        @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(FLIP); }
        @Override protected VoxelShape getOutlineShape(BlockState st, BlockView w, BlockPos p, net.minecraft.block.ShapeContext c) { return SHAPE; }
    }

    public static final Block MIRROR = register("sun_mirror", new MirrorBlock(AbstractBlock.Settings.create().strength(-1f, 3600000f).nonOpaque()
            .sounds(BlockSoundGroup.GLASS).luminance(s -> 3).pistonBehavior(PistonBehavior.BLOCK).dropsNothing()));
    public static final Block RUNE_STONE = register("rune_stone", new Block(AbstractBlock.Settings.create().strength(-1f, 3600000f)
            .sounds(BlockSoundGroup.DEEPSLATE).luminance(s -> 6).pistonBehavior(PistonBehavior.BLOCK).dropsNothing()));
    public static final Block SINGING_STONE = register("singing_stone", new Block(AbstractBlock.Settings.create().strength(-1f, 3600000f)
            .sounds(BlockSoundGroup.STONE).pistonBehavior(PistonBehavior.BLOCK).dropsNothing()));

    private static Block register(String id, Block b) {
        Identifier i = Identifier.of(ForgottenIsland.MOD_ID, id);
        Registry.register(Registries.BLOCK, i, b);
        Registry.register(Registries.ITEM, i, new BlockItem(b, new Item.Settings()));
        return b;
    }
    public static void init() {}
}
