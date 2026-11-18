package dev.forgottenisland;

import dev.forgottenisland.content.Content;

import java.nio.file.*;
import java.util.stream.Stream;

/** Loads the shipped content straight from the source tree. */
public final class TestContent {
    private TestContent() {}
    static final Path ROOT = Paths.get("src/main/resources/data/forgotten_island/fi");
    private static Content cached;

    public static synchronized Content load() throws Exception {
        TestBoot.boot();
        if (cached != null) return cached;
        Content c = new Content();
        try (Stream<Path> files = Files.list(ROOT.resolve("dialogue"))) {
            for (Path p : files.sorted().toList()) c.addDialogue(Files.readString(p));
        }
        c.addQuests(Files.readString(ROOT.resolve("quests.json")));
        c.addSchedules(Files.readString(ROOT.resolve("schedules.json")));
        c.addBooks(Files.readString(ROOT.resolve("books.json")));
        c.addPeople(Files.readString(ROOT.resolve("people.json")));
        cached = c;
        return c;
    }
}
