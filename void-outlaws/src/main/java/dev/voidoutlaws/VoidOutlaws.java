package dev.voidoutlaws;

import dev.voidoutlaws.cmd.Commands;
import dev.voidoutlaws.game.Hooks;
import dev.voidoutlaws.reg.ModBlocks;
import dev.voidoutlaws.reg.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoidOutlaws implements ModInitializer {
    public static final String MODID = "voidoutlaws";
    public static final Logger LOG = LoggerFactory.getLogger("Void Outlaws");

    public static Identifier id(String path) {
        return Identifier.of(MODID, path);
    }

    @Override
    public void onInitialize() {
        ModBlocks.init();
        ModItems.init();
        Commands.init();
        Hooks.init();
        LOG.info("Void Outlaws loaded. Saddle up, space-weasels.");
    }
}
