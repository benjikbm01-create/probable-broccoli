package dev.forgottenisland.game;

import dev.forgottenisland.ForgottenIsland;
import dev.forgottenisland.content.Content;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.util.Map;
import java.util.stream.Collectors;

/** Loads game content from data/forgotten_island/fi/ (bundled in the mod jar, overridable by datapacks). */
public final class ContentHolder {
    private static Content current = new Content();
    private ContentHolder() {}
    public static Content get() { return current; }

    public static void register() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override public Identifier getFabricId() { return Identifier.of(ForgottenIsland.MOD_ID, "content"); }
            @Override public void reload(ResourceManager rm) {
                Content c = new Content();
                try {
                    for (Map.Entry<Identifier, Resource> e : rm.findResources("fi/dialogue", id -> id.getPath().endsWith(".json")).entrySet())
                        c.addDialogue(read(e.getValue()));
                    one(rm, "fi/quests.json", c::addQuests);
                    one(rm, "fi/schedules.json", c::addSchedules);
                    one(rm, "fi/books.json", c::addBooks);
                    one(rm, "fi/people.json", c::addPeople);
                    var errs = c.validate(Places.names(), KeyItems.ids(), Story.eventNames());
                    for (String err : errs) ForgottenIsland.LOG.error("Content problem: {}", err);
                    ForgottenIsland.LOG.info("Loaded content: {} dialogues, {} objectives, {} schedules, {} books ({} problems)",
                            c.dialogues.size(), c.objectives.size(), c.schedules.size(), c.books.size(), errs.size());
                    current = c;
                } catch (Exception ex) {
                    ForgottenIsland.LOG.error("Failed to load Forgotten Island content", ex);
                }
            }
        });
    }

    private static void one(ResourceManager rm, String path, java.util.function.Consumer<String> sink) throws Exception {
        var r = rm.getResource(Identifier.of(ForgottenIsland.MOD_ID, path));
        if (r.isPresent()) sink.accept(read(r.get()));
        else ForgottenIsland.LOG.warn("Missing content file {}", path);
    }

    private static String read(Resource r) throws Exception {
        try (BufferedReader br = r.getReader()) { return br.lines().collect(Collectors.joining("\n")); }
    }
}
