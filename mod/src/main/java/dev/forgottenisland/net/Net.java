package dev.forgottenisland.net;

import dev.forgottenisland.ForgottenIsland;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

/** Network payloads. The server is authoritative; the client only renders and reports choices. */
public final class Net {
    private Net() {}

    /** Open/refresh the dialogue screen. options is never empty (server adds Continue/Leave). */
    public record Dialogue(String npc, String name, String text, List<String> options) implements CustomPayload {
        public static final Id<Dialogue> ID = new Id<>(Identifier.of(ForgottenIsland.MOD_ID, "dialogue"));
        public static final PacketCodec<RegistryByteBuf, Dialogue> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, Dialogue::npc, PacketCodecs.STRING, Dialogue::name, PacketCodecs.STRING, Dialogue::text,
                PacketCodecs.STRING.collect(PacketCodecs.toList()), Dialogue::options, Dialogue::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** Generic UI message: kind = close | journal | hud | intro | credits | fade | toast | hint; data = JSON or text. */
    public record Ui(String kind, String data) implements CustomPayload {
        public static final Id<Ui> ID = new Id<>(Identifier.of(ForgottenIsland.MOD_ID, "ui"));
        public static final PacketCodec<RegistryByteBuf, Ui> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, Ui::kind, PacketCodecs.STRING, Ui::data, Ui::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** Client action: kind = choice | close | journal | hint | intro_done | credits_done. */
    public record Action(String kind, int value) implements CustomPayload {
        public static final Id<Action> ID = new Id<>(Identifier.of(ForgottenIsland.MOD_ID, "action"));
        public static final PacketCodec<RegistryByteBuf, Action> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, Action::kind, PacketCodecs.VAR_INT, Action::value, Action::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(Dialogue.ID, Dialogue.CODEC);
        PayloadTypeRegistry.playS2C().register(Ui.ID, Ui.CODEC);
        PayloadTypeRegistry.playC2S().register(Action.ID, Action.CODEC);
    }
}
