package dev.voidoutlaws.reg;

import dev.voidoutlaws.game.Interact;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** All interactive mod blocks share this class; behaviour is dispatched by {@link Kind}. */
public class OutlawBlock extends Block {
    public enum Kind {
        SHIP_CONSOLE("ship_console", 12),
        GUNNERY_STATION("gunnery_station", 10),
        ENGINEERING_PANEL("engineering_panel", 10),
        CARGO_HOLD("cargo_hold", 0),
        HIDDEN_COMPARTMENT("hidden_compartment", 0),
        SHIP_HATCH("ship_hatch", 4),
        DOCKING_BEACON("docking_beacon", 15),
        JOB_BOARD("job_board", 6),
        DEAD_DROP("dead_drop", 0),
        BLACK_MARKET("black_market", 8),
        CARD_TABLE("card_table", 4),
        VAULT_TERMINAL("vault_terminal", 10),
        LOOT_CRATE("loot_crate", 0),
        LOOT_LOCKER("loot_locker", 6),
        FINE_KIOSK("fine_kiosk", 8),
        BULKHEAD_PANEL("bulkhead_panel", 9);

        public final String id;
        public final int light;

        Kind(String id, int light) {
            this.id = id;
            this.light = light;
        }
    }

    public final Kind kind;

    public OutlawBlock(Kind kind, Settings settings) {
        super(settings);
        this.kind = kind;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (player instanceof ServerPlayerEntity sp) Interact.useBlock(kind, sp, pos);
        return ActionResult.CONSUME;
    }
}
