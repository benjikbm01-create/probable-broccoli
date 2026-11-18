package dev.forgottenisland.client;

import dev.forgottenisland.client.screen.*;
import dev.forgottenisland.net.Net;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;

/** Client side of the networking: renders what the server sends, reports what the player chooses. */
public final class ClientNet {
    private ClientNet() {}

    public static void action(String kind, int value) {
        if (ClientPlayNetworking.canSend(Net.Action.ID)) ClientPlayNetworking.send(new Net.Action(kind, value));
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(Net.Dialogue.ID, (p, ctx) -> {
            MinecraftClient mc = ctx.client();
            ClientState.lastUiKind = "dialogue";
            if (mc.currentScreen instanceof DialogueScreen ds) ds.set(p.npc(), p.name(), p.text(), p.options());
            else mc.setScreen(new DialogueScreen(p.npc(), p.name(), p.text(), p.options()));
        });
        ClientPlayNetworking.registerGlobalReceiver(Net.Ui.ID, (p, ctx) -> {
            MinecraftClient mc = ctx.client();
            ClientState.lastUiKind = p.kind();
            switch (p.kind()) {
                case "close" -> { if (mc.currentScreen instanceof DialogueScreen ds) ds.serverClose(); }
                case "journal" -> { if (mc.currentScreen instanceof JournalScreen js) js.update(p.data()); else mc.setScreen(new JournalScreen(p.data())); }
                case "hud" -> ClientState.hud(p.data());
                case "intro" -> mc.setScreen(new IntroScreen());
                case "credits" -> mc.setScreen(new CreditsScreen(p.data()));
                case "fade" -> ClientState.fade(p.data());
                case "toast" -> {
                    String[] s = p.data().split("\n", 2);
                    ClientState.toast(s[0], s.length > 1 ? s[1] : "", 5500);
                }
                case "hint" -> {
                    String[] s = p.data().split("\n", 2);
                    ClientState.toast("Hint: " + s[0], s.length > 1 ? s[1] : "", 9000);
                    if (mc.player != null) mc.player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                    if (mc.currentScreen instanceof JournalScreen) action("journal", 0);
                }
                default -> {}
            }
        });
    }
}
