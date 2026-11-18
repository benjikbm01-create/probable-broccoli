package dev.forgottenisland;

import dev.forgottenisland.dialogue.DialogueManager;
import dev.forgottenisland.game.*;
import dev.forgottenisland.net.Net;
import dev.forgottenisland.npc.ModEntities;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForgottenIsland implements ModInitializer {
    public static final String MOD_ID = "forgotten_island";
    public static final Logger LOG = LoggerFactory.getLogger("ForgottenIsland");

    @Override
    public void onInitialize() {
        Net.register();
        ModItems.init();
        ModBlocks.init();
        ModEntities.init();
        ContentHolder.register();
        Puzzles.register();
        Protection.register();
        Commands.register();

        ServerLifecycleEvents.SERVER_STARTED.register(Game::onServerStart);
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> Game.onServerStop());
        ServerTickEvents.END_SERVER_TICK.register(Game::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> Story.onJoin(handler.player)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> DialogueManager.drop(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> {
            if (alive) return;
            DialogueManager.drop(newP);
            Encounters.resetActive(newP.getServer());
            Story.abortBoss(newP.getServer());
            Checkpoints.respawn(newP);
            Game.syncHud(newP, true);
        });
        ServerPlayNetworking.registerGlobalReceiver(Net.Action.ID, (payload, ctx) -> {
            var p = ctx.player();
            switch (payload.kind()) {
                case "choice" -> DialogueManager.choose(p, payload.value());
                case "close" -> DialogueManager.drop(p);
                case "journal" -> Journal.open(p);
                case "hint" -> Journal.hint(p);
                case "intro_done" -> Story.finishIntro(p);
                case "credits_done" -> Story.finishCredits(p);
                case "hud" -> { var st = Game.state(p.getServer()); if (st.flag("hud_off")) st.clear("hud_off"); else st.set("hud_off"); Game.syncHud(p, true); }
                default -> LOG.warn("Unknown action {}", payload.kind());
            }
        });
        LOG.info("The Forgotten Island loaded");
    }
}
