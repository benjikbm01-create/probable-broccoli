package dev.forgottenisland.client.screen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.forgottenisland.client.ClientNet;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Scrolling end credits with a short, personalised epilogue. */
public class CreditsScreen extends Screen {
    private final List<String[]> lines = new ArrayList<>();   // {style, text}
    private final long start = System.currentTimeMillis();
    private boolean sent;

    public CreditsScreen(String json) {
        super(Text.literal("Credits"));
        JsonObject j = JsonParser.parseString(json).getAsJsonObject();
        add("T", "The Forgotten Island");
        add("", ""); add("", "");
        add("H", "And so the island was remembered.");
        add("", "");
        epilogue(j, "wren", "Wren Ashdown", "walks the Mosswood with you at dawn, and lets you name the owls.",
                "still keeps the Mosswood. She saves you the best seat at every fire.", "nods to you on the forest road, as one ranger to another.");
        epilogue(j, "ione", "Ione Calder", "has started a new chapter of the Chronicle. You are on every page.",
                "has written you into the Chronicle as \"the friend who stayed\".", "sends you a note now and then. The handwriting is very neat.");
        epilogue(j, "sol", "Sol Reyes", "is planning a voyage past the edge of every map, and she has saved you a berth.",
                "drags you out sailing every fair-weather morning. You've stopped pretending to mind.", "waves from the rigging whenever you pass the harbour.");
        add("", "");
        add("", "Secrets discovered: " + j.get("secrets").getAsInt() + " of " + j.get("secretTotal").getAsInt());
        add("", "Islanders helped: " + j.get("side").getAsInt() + " of 3");
        add("", ""); add("", ""); add("", "");
        add("H", "Created for Minecraft Java Edition 1.21.1");
        add("", "Story, world, characters and skins: original work for this project");
        add("", "Built with Fabric Loader and Fabric API (Apache-2.0)");
        add("", "Minecraft is a trademark of Mojang Studios. Not an official Minecraft product.");
        add("", ""); add("", "");
        add("H", "Thank you for playing.");
        add("", ""); add("", ""); add("", ""); add("", "");
    }

    private void epilogue(JsonObject j, String id, String name, String dating, String friend, String other) {
        String s = j.get(id).getAsString();
        if (s.equals("dating")) add("", name + " " + dating);
        else if (s.equals("friend")) add("", name + " " + friend);
        else if (!s.isEmpty()) add("", name + " " + other);
        else return;
        add("", "");
    }
    private void add(String style, String text) { lines.add(new String[]{style, text}); }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return System.currentTimeMillis() - start > 4000; }
    @Override public void renderBackground(DrawContext ctx, int mx, int my, float d) { ctx.fill(0, 0, width, height, 0xFF040608); }

    @Override public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        float t = (System.currentTimeMillis() - start) / 1000f;
        int y = (int) (height - t * 22);
        int w = Math.min(width - 40, 360);
        for (String[] l : lines) {
            if (l[0].equals("T")) {
                float sc = Math.min(2.5f, (width - 24f) / textRenderer.getWidth(Text.literal(l[1]).formatted(Formatting.BOLD)));
                var m = ctx.getMatrices(); m.push(); m.translate(width / 2f, y, 0); m.scale(sc, sc, 1);
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(l[1]).formatted(Formatting.BOLD), 0, 0, 0xFFE8C170);
                m.pop(); y += 30; continue;
            }
            Text tx = l[0].equals("H") ? Text.literal(l[1]).formatted(Formatting.BOLD) : Text.literal(l[1]);
            int color = l[0].equals("H") ? 0xFFE8C170 : 0xFFEFE6D2;
            for (OrderedText o : textRenderer.wrapLines(tx, w)) { ctx.drawCenteredTextWithShadow(textRenderer, o, width / 2, y, color); y += 11; }
        }
        if (y < -10) close();
        if (t > 4) ctx.drawText(textRenderer, Text.literal("Esc to skip").formatted(Formatting.GRAY), 6, height - 12, 0xFF808080, false);
        super.render(ctx, mx, my, d);
    }

    @Override public void close() {
        if (!sent) { sent = true; ClientNet.action("credits_done", 0); }
        super.close();
    }
}
