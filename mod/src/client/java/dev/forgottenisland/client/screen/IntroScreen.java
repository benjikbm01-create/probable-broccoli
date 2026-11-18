package dev.forgottenisland.client.screen;

import dev.forgottenisland.client.ClientNet;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Title card, premise and controls, shown once at the very start. */
public class IntroScreen extends Screen {
    private final long opened = System.currentTimeMillis();
    private int page;
    static final String PREMISE = "The letter came to you in a bottle, washed up on a mainland shore. It was signed with two initials, "
            + "and it asked you to come to an island that no chart remembers.\n\nYou hired a little boat and rowed into the fog. "
            + "For a day and a night there was nothing but grey.\n\nNow the fog is thinning. There are lights ahead.";
    static final String[][] CONTROLS = {
            {"Move / look", "WASD + mouse (your usual Minecraft controls)"},
            {"Talk to someone", "Right-click them, then pick an answer (mouse or 1-9)"},
            {"Journal", "J  (or right-click the journal item)"},
            {"Hint when stuck", "H  (or the button in the journal)"},
            {"Hide objective tracker", "O"},
            {"Interact with the world", "Right-click doors, levers, chests, stones, mirrors..."},
            {"Rest / pass time", "Sleep in your bed at the inn, or ask the innkeeper"},
            {"If you fall", "You return at the last checkpoint and keep your items"}};

    public IntroScreen() { super(Text.literal("The Forgotten Island")); }

    @Override protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal(page == 0 ? "Continue" : "Begin"), b -> {
            if (page == 0) { page = 1; clearAndInit(); } else close();
        }).dimensions(width / 2 - 60, height - 34, 120, 20).build());
    }

    @Override public boolean shouldPause() { return false; }

    @Override public void renderBackground(DrawContext ctx, int mx, int my, float d) { ctx.fill(0, 0, width, height, 0xFF07090C); }

    @Override public void render(DrawContext ctx, int mx, int my, float d) {
        super.render(ctx, mx, my, d);
        float t = Math.min(1f, (System.currentTimeMillis() - opened) / 2500f);
        int a = (int) (t * 255) << 24;
        var m = ctx.getMatrices();
        m.push();
        m.translate(width / 2f, 26, 0);
        float sc = Math.min(3f, (width - 24f) / textRenderer.getWidth(Text.literal("The Forgotten Island").formatted(Formatting.BOLD)));
        m.scale(sc, sc, 1f);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("The Forgotten Island").formatted(Formatting.BOLD), 0, 0, a | 0xE8C170);
        m.pop();
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("a short adventure on the Isle of Ysmere").formatted(Formatting.ITALIC), width / 2, 60, a | 0xA8A090);
        int y = 82, w = Math.min(width - 40, 380), x = (width - w) / 2;
        if (page == 0) {
            for (String para : PREMISE.split("\n")) {
                for (OrderedText l : textRenderer.wrapLines(Text.literal(para), w)) { ctx.drawCenteredTextWithShadow(textRenderer, l, width / 2, y, a | 0xEFE6D2); y += 11; }
            }
        } else {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Controls").formatted(Formatting.BOLD, Formatting.UNDERLINE), width / 2, y, 0xFFE8C170);
            y += 16;
            for (String[] c : CONTROLS) {
                ctx.drawText(textRenderer, Text.literal(c[0]), x, y, 0xFFE8C170, true);
                for (OrderedText l : textRenderer.wrapLines(Text.literal(c[1]), w - 150)) { ctx.drawText(textRenderer, l, x + 150, y, 0xFFEFE6D2, true); y += 10; }
                y += 3;
            }
        }
    }

    @Override public void close() { ClientNet.action("intro_done", 0); super.close(); }
}
