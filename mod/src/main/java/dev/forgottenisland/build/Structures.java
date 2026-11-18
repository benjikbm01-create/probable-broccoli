package dev.forgottenisland.build;

import java.util.ArrayList;
import java.util.List;

/** All structure parts in build order (after terrain). */
public final class Structures {
    private Structures() {}
    public static List<Layout.Part> parts() {
        List<Layout.Part> l = new ArrayList<>();
        l.add(new Inn());
        l.add(new Harbor());
        l.add(new Town());
        l.add(new Keep());
        l.add(new Ruins());
        l.add(new Forest());
        return l;
    }
}
