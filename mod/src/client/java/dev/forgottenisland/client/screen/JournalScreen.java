package dev.forgottenisland.client.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.forgottenisland.client.ClientNet;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** The traveller's journal: objectives, people, secrets, hints. */
public class JournalScreen extends Screen {
    private JsonObject data;
    private int tab, scroll;
    private static int lastTab;
    static final String[] TABS = {"Objectives", "People", "Secrets", "Hints"};
    static final int INK = 0xFF3B2A1C, INK_LIGHT = 0xFF7A6248, PAGE = 0xFFF1E6CC, RED = 0xFF8E2A20;

    public JournalScreen(String json) { super(Text.literal("Journal")); this.data = JsonParser.parseString(json).getAsJsonObject(); this.tab = lastTab; }
    public void update(String json) { this.data = JsonParser.parseString(json).getAsJsonObject(); }

    @Override public boolean shouldPause() { return false; }

    int pw() { return Math.min(width - 20, 360); }
    int ph() { return Math.min(height - 20, 230); }
    int px() { return (width - pw()) / 2; }
    int py() { return (height - ph()) / 2; }

    @Override protected void init() {
        int x = px(), y = py();
        for (int i = 0; i < TABS.length; i++) {
            int t = i;
            addDrawableChild(ButtonWidget.builder(Text.literal(TABS[i]), b -> { tab = t; lastTab = t; scroll = 0; })
                    .dimensions(x + 6 + i * ((pw() - 12) / 4), y - 20, (pw() - 12) / 4 - 2, 18).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Reveal a hint"), b -> { ClientNet.action("hint", 0); tab = 3; lastTab = 3; })
                .dimensions(x + pw() - 96, y + ph() + 2, 94, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close()).dimensions(x, y + ph() + 2, 60, 18).build());
    }

    @Override public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        int x = px(), y = py(), w = pw(), h = ph();
        ctx.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0xFF5A3A22);
        ctx.fill(x, y, x + w, y + h, PAGE);
        ctx.fill(x + w / 2, y + 4, x + w / 2 + 1, y + h - 4, 0x30000000);
        List<Line> lines = build(w - 24);
        int lh = 10, visible = (h - 34) / lh;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, lines.size() - visible)));
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(TABS[tab]).formatted(Formatting.BOLD), x + w / 2, y + 7, 0xFFE8C170);
        ctx.fill(x + 12, y + 18, x + w - 12, y + 19, INK_LIGHT);
        ctx.enableScissor(x, y + 22, x + w, y + h - 6);
        int ly = y + 24;
        for (int i = scroll; i < lines.size() && i < scroll + visible; i++) {
            Line l = lines.get(i);
            ctx.drawText(textRenderer, l.text, x + 12 + l.indent, ly, l.color, false);
            ly += lh;
        }
        ctx.disableScissor();
        if (lines.size() > visible) ctx.drawText(textRenderer, Text.literal("scroll ↕"), x + w - 50, y + h - 12, INK_LIGHT, false);
        if (data.has("dev") && data.get("dev").getAsBoolean()) ctx.drawText(textRenderer, Text.literal("DEV MODE"), x + 6, y + h - 12, RED, false);
        super.render(ctx, mx, my, d);
    }

    @Override public boolean mouseScrolled(double mx, double my, double hx, double vy) { scroll -= (int) Math.signum(vy) * 2; return true; }

    record Line(OrderedText text, int color, int indent) {}

    private List<Line> build(int width) {
        List<Line> out = new ArrayList<>();
        switch (tab) {
            case 0 -> {
                section(out, "Main story", data.getAsJsonArray("main"), width);
                section(out, "Side quests", data.getAsJsonArray("side"), width);
                section(out, "Personal", data.getAsJsonArray("personal"), width);
                if (data.get("finished").getAsBoolean()) para(out, "The story is complete. Gullhaven is yours to wander.", width, RED, 0);
            }
            case 1 -> {
                for (JsonElement e : data.getAsJsonArray("people")) {
                    JsonObject o = e.getAsJsonObject();
                    para(out, o.get("name").getAsString() + "  —  " + o.get("status").getAsString(), width, INK, 0);
                    para(out, o.get("role").getAsString(), width - 8, INK_LIGHT, 8);
                    if (!o.get("line").getAsString().isEmpty()) para(out, o.get("line").getAsString(), width - 8, INK_LIGHT, 8);
                    out.add(new Line(Text.empty().asOrderedText(), INK, 0));
                }
            }
            case 2 -> {
                para(out, "Secrets found: " + data.get("secretFound").getAsInt() + " of " + data.get("secretTotal").getAsInt(), width, INK, 0);
                out.add(new Line(Text.empty().asOrderedText(), INK, 0));
                for (JsonElement e : data.getAsJsonArray("secrets")) para(out, "✦ " + e.getAsString(), width, INK, 4);
                if (data.getAsJsonArray("secrets").isEmpty()) para(out, "Look in the places others overlook: high, low, behind and beneath.", width, INK_LIGHT, 0);
            }
            default -> {
                JsonObject hnt = data.getAsJsonObject("hint");
                if (!hnt.has("for")) { para(out, "Nothing urgent. Explore, talk to people, look for secrets.", width, INK_LIGHT, 0); break; }
                para(out, "Hints for: " + hnt.get("for").getAsString(), width, INK, 0);
                out.add(new Line(Text.empty().asOrderedText(), INK, 0));
                JsonArray shown = hnt.getAsJsonArray("shown");
                if (shown.isEmpty()) para(out, "No hints revealed yet. Press \"Reveal a hint\" (or H) if you're stuck.", width, INK_LIGHT, 0);
                int i = 1;
                for (JsonElement e : shown) para(out, (i++) + ". " + e.getAsString(), width, INK, 0);
                if (!hnt.get("more").getAsBoolean() && !shown.isEmpty()) para(out, "(That's every hint for this objective.)", width, INK_LIGHT, 0);
            }
        }
        return out;
    }

    private void section(List<Line> out, String title, JsonArray arr, int width) {
        if (arr.isEmpty()) return;
        out.add(new Line(Text.literal(title).formatted(Formatting.BOLD, Formatting.UNDERLINE).asOrderedText(), RED, 0));
        for (int pass = 0; pass < 2; pass++)
            for (JsonElement e : arr) {
                JsonObject o = e.getAsJsonObject();
                boolean done = o.get("done").getAsBoolean();
                if ((pass == 0) == done) continue;
                para(out, (done ? "✔ " : "◆ ") + o.get("t").getAsString(), width, done ? INK_LIGHT : INK, 0);
                if (!done) para(out, o.get("d").getAsString(), width - 10, INK_LIGHT, 10);
            }
        out.add(new Line(Text.empty().asOrderedText(), INK, 0));
    }

    private void para(List<Line> out, String s, int width, int color, int indent) {
        for (OrderedText t : textRenderer.wrapLines(Text.literal(s), width - indent)) out.add(new Line(t, color, indent));
    }
}
