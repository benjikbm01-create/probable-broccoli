package dev.forgottenisland.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Objective tracker (top-left), story toasts (top-right) and full-screen fades. */
public final class HudOverlay {
    private HudOverlay() {}
    static final int GOLD = 0xFFE8C170, PARCH = 0xFFEFE6D2;

    public static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        TextRenderer tr = mc.textRenderer;
        int W = ctx.getScaledWindowWidth(), H = ctx.getScaledWindowHeight();
        if (!mc.options.hudHidden && ClientState.hudVisible && ClientState.hudAllowed && ClientState.introDone && !ClientState.objTitle.isEmpty() && mc.currentScreen == null) {
            List<OrderedText> lines = tr.wrapLines(Text.literal(ClientState.objText), 170);
            int h = 16 + lines.size() * 10;
            ctx.fill(4, 4, 186, 6 + h, 0x88101418);
            ctx.fill(4, 4, 5, 6 + h, GOLD);
            ctx.drawText(tr, Text.literal("◆ " + ClientState.objTitle).formatted(Formatting.BOLD), 9, 8, GOLD, true);
            int y = 20;
            for (OrderedText l : lines) { ctx.drawText(tr, l, 9, y, 0xFFD8D2C4, true); y += 10; }
        }
        // toasts
        ClientState.Toast t = ClientState.toasts.peekFirst();
        if (t != null) {
            long now = System.currentTimeMillis();
            if (t.start() < 0) { ClientState.toasts.pollFirst(); t = new ClientState.Toast(t.title(), t.text(), now, t.dur()); ClientState.toasts.addFirst(t); }
            long age = now - t.start();
            if (age > t.dur()) ClientState.toasts.pollFirst();
            else {
                float slide = Math.min(1f, Math.min(age, t.dur() - age) / 300f);
                List<OrderedText> lines = tr.wrapLines(Text.literal(t.text()), 190);
                int w = 210, h = 18 + lines.size() * 10;
                int x = W - (int) (w * slide) - 4, y = 4;
                ctx.fill(x, y, x + w, y + h, 0xE0151A20);
                ctx.drawBorder(x, y, w, h, GOLD);
                ctx.drawText(tr, Text.literal(t.title()).formatted(Formatting.BOLD), x + 8, y + 5, GOLD, true);
                int ly = y + 16;
                for (OrderedText l : lines) { ctx.drawText(tr, l, x + 8, ly, PARCH, false); ly += 10; }
            }
        }
        // fade to black with a line of text
        if (ClientState.fadeText != null) {
            long age = System.currentTimeMillis() - ClientState.fadeStart;
            float a;
            if (age < 1200) a = age / 1200f; else if (age < 4200) a = 1f; else if (age < 5400) a = 1f - (age - 4200) / 1200f; else { ClientState.fadeText = null; return; }
            int alpha = (int) (a * 255) << 24;
            ctx.fill(0, 0, W, H, alpha);
            if (a > 0.6f) {
                List<OrderedText> lines = tr.wrapLines(Text.literal(ClientState.fadeText).formatted(Formatting.ITALIC), Math.min(320, W - 40));
                int y = H / 2 - lines.size() * 6;
                int ta = (int) (Math.min(1f, (a - 0.6f) / 0.4f) * 255);
                for (OrderedText l : lines) { ctx.drawCenteredTextWithShadow(tr, l, W / 2, y, (ta << 24) | 0xEFE6D2); y += 12; }
            }
        }
    }
}
