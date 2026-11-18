package dev.forgottenisland.game;

import dev.forgottenisland.npc.IslandNpcEntity;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.*;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.ActionResult;

/** Keeps the handcrafted island intact: no block breaking, no stripping decorations, no fiddling with props. */
public final class Protection {
    private Protection() {}

    static boolean exempt(PlayerEntity p) { return p.isCreative(); }

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> exempt(player));
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (exempt(player)) return ActionResult.PASS;
            if (entity instanceof ItemFrameEntity || entity instanceof ArmorStandEntity || entity instanceof PaintingEntity || entity instanceof IslandNpcEntity)
                return ActionResult.FAIL;
            if (entity instanceof BoatEntity && entity.getCommandTags().contains("fi_deco")) return ActionResult.FAIL;
            return ActionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (exempt(player)) return ActionResult.PASS;
            if (entity instanceof ItemFrameEntity || entity instanceof ArmorStandEntity) return ActionResult.FAIL;
            if (entity instanceof BoatEntity && entity.getCommandTags().contains("fi_deco")) return ActionResult.FAIL;
            return ActionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (exempt(player)) return ActionResult.PASS;
            Block b = world.getBlockState(hit.getBlockPos()).getBlock();
            if (b instanceof FlowerPotBlock || b instanceof NoteBlock || b instanceof RepeaterBlock || b instanceof ComparatorBlock
                    || b instanceof DaylightDetectorBlock || b instanceof ChiseledBookshelfBlock || b instanceof DecoratedPotBlock
                    || b instanceof CakeBlock || b instanceof ComposterBlock || b instanceof AbstractCauldronBlock || b instanceof JukeboxBlock
                    || b instanceof CandleBlock || b instanceof CandleCakeBlock || b instanceof RespawnAnchorBlock || b instanceof CaveVinesBodyBlock
                    || b instanceof CaveVinesHeadBlock || b instanceof SweetBerryBushBlock || b instanceof BeehiveBlock || b instanceof VaultBlock
                    || b instanceof CampfireBlock && !Puzzles.handlers().containsKey(hit.getBlockPos()))
                return ActionResult.FAIL;
            return ActionResult.PASS;
        });
    }
}
