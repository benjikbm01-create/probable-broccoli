package dev.voidoutlaws.reg;

import dev.voidoutlaws.game.Gear;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class GearItem extends Item {
    public enum Kind { CONTRABAND, BLASTER, JETPACK, GRAPPLE, DISGUISE, TRANSPONDER, HULL_PATCH, WHISTLE, TOMATO, HANDBOOK, LEDGER }

    public final Kind kind;
    private final String tip;

    public GearItem(Kind kind, Settings settings, String tip) {
        super(settings);
        this.kind = kind;
        this.tip = tip;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (kind == Kind.CONTRABAND || kind == Kind.TRANSPONDER) return TypedActionResult.pass(stack);
        if (world.isClient) return TypedActionResult.success(stack);
        if (user instanceof ServerPlayerEntity sp && Gear.use(kind, sp, hand, stack)) return TypedActionResult.success(stack);
        return TypedActionResult.pass(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (kind == Kind.CONTRABAND) tooltip.add(Text.literal("Contraband! Hide it from scans.").formatted(Formatting.RED));
        tooltip.add(Text.literal(tip).formatted(Formatting.GRAY));
    }
}
