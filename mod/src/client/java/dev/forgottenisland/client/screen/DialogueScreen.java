package dev.forgottenisland.client.screen;

import dev.forgottenisland.client.ClientNet;
import dev.forgottenisland.npc.IslandNpcEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.Entity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Conversation UI: portrait, name, typewriter text, numbered choices. Server decides everything. */
public class DialogueScreen extends Screen {
    public String npc, name, text;
    public List<String> options = new ArrayList<>();
    private long shownAt;
    private boolean waiting;
    private int hovered = -1;
    private final List<int[]> optionBoxes = new ArrayList<>();
    static final int GOLD = 0xFFE8C170;

    public DialogueScreen(String npc, String name, String text, List<String> options) {
        super(Text.literal(name));
        set(npc, name, text, options);
    }

    public void set(String npc, String name, String text, List<String> options) {
        this.npc = npc; this.name = name; this.text = text; this.options = new ArrayList<>(options);
        shownAt = System.currentTimeMillis(); waiting = false;
    }

    @Override public boolean shouldPause() { return false; }

    private int revealed() {
        long ms = System.currentTimeMillis() - shownAt;
        return (int) Math.min(text.length(), ms / 14);
    }
    private boolean fullyShown() { return revealed() >= text.length(); }

    private IslandNpcEntity findNpc() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return null;
        IslandNpcEntity best = null; double bd = 400;
        for (Entity e : mc.world.getEntities())
            if (e instanceof IslandNpcEntity n && npc.equals(n.npcId()) && e.squaredDistanceTo(mc.player) < bd) { best = n; bd = e.squaredDistanceTo(mc.player); }
        return best;
    }

    @Override public void render(DrawContext ctx, int mx, int my, float delta) {
        int W = width, H = height;
        int pw = Math.min(W - 20, 460), ph = Math.max(130, Math.min(H / 2, 190));
        int px = (W - pw) / 2, py = H - ph - 8;
        ctx.fillGradient(0, py - 40, W, H, 0x00000000, 0xC0000000);
        ctx.fill(px, py, px + pw, py + ph, 0xE0141820);
        ctx.drawBorder(px, py, pw, ph, GOLD);
        ctx.drawBorder(px + 2, py + 2, pw - 4, ph - 4, 0x60E8C170);
        int tx = px + 12;
        IslandNpcEntity e = findNpc();
        if (e != null) {
            int bx = px + 10, by = py + 10, bw = 70, bh = 92;
            ctx.fill(bx, by, bx + bw, by + bh, 0xFF22303A);
            ctx.drawBorder(bx, by, bw, bh, 0xA0E8C170);
            ctx.enableScissor(bx + 1, by + 1, bx + bw - 1, by + bh - 1);
            InventoryScreen.drawEntity(ctx, bx, by, bx + bw, by + bh + 50, 48, 0.0625f, bx + bw / 2f, by + 20, e);
            ctx.disableScissor();
            tx = bx + bw + 12;
        }
        int tw = px + pw - 12 - tx;
        ctx.drawText(textRenderer, Text.literal(name).formatted(Formatting.BOLD), tx, py + 10, GOLD, true);
        String shown = text.substring(0, revealed());
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(shown), tw);
        int y = py + 24;
        for (OrderedText l : lines) { ctx.drawText(textRenderer, l, tx, y, 0xFFEFE6D2, false); y += 10; }
        optionBoxes.clear();
        hovered = -1;
        if (fullyShown()) {
            int oy = Math.max(y + 8, py + ph - 8 - options.size() * 13);
            for (int i = 0; i < options.size(); i++) {
                String label = (i + 1) + ". " + options.get(i);
                List<OrderedText> ol = textRenderer.wrapLines(Text.literal(label), tw - 6);
                int oh = ol.size() * 10 + 3;
                boolean hov = mx >= tx - 3 && mx <= tx + tw && my >= oy - 2 && my < oy + oh - 1;
                if (hov) { hovered = i; ctx.fill(tx - 3, oy - 2, tx + tw, oy + oh - 2, 0x40E8C170); }
                int oyy = oy;
                for (OrderedText l : ol) { ctx.drawText(textRenderer, l, tx, oyy, hov ? 0xFFFFFFFF : (waiting ? 0xFF808080 : 0xFFE8C170), false); oyy += 10; }
                optionBoxes.add(new int[]{oy, oy + oh});
                oy += oh + 1;
            }
        } else {
            ctx.drawText(textRenderer, Text.literal("▼").formatted(Formatting.GRAY), px + pw - 16, py + ph - 14, 0xFFB0A080, false);
        }
        super.render(ctx, mx, my, delta);
    }

    @Override public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (!fullyShown()) { shownAt = 0; return true; }
        if (hovered >= 0) { choose(hovered); return true; }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) { if (fullyShown()) choose(key - GLFW.GLFW_KEY_1); else shownAt = 0; return true; }
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) { if (!fullyShown()) shownAt = 0; else if (options.size() == 1) choose(0); return true; }
        return super.keyPressed(key, scan, mods);
    }

    public void choose(int i) {
        if (waiting || i < 0 || i >= options.size()) return;
        waiting = true;
        ClientNet.action("choice", i);
    }

    @Override public void close() {
        ClientNet.action("close", 0);
        super.close();
    }

    /** Closed by the server: don't echo a close back. */
    public void serverClose() { super.close(); }
}
