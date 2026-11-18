package dev.forgottenisland;

import dev.forgottenisland.core.IslandState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.BuiltinRegistries;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Plays the real dialogue files through complete story routes. */
class StoryPathTest {

    static DialogueSim fresh() throws Exception {
        DialogueSim s = new DialogueSim(TestContent.load());
        s.state.set("intro_done");
        return s;
    }

    /** Arrival through the letter being identified. */
    static void act1(DialogueSim s) {
        assertTrue(s.active("m1"));
        s.talk("tobias"); s.next(); s.choose("letter"); s.finish();
        assertTrue(s.state.flag("shown_letter_tobias"));
        s.talk("hester"); s.choose("room"); s.next(); s.finish();
        assertTrue(s.state.flag("room_given"));
        s.talk("ione"); s.choose("seal"); s.finish();
        assertTrue(s.done("m1")); assertTrue(s.active("m2"));
    }

    static void learnSong(DialogueSim s) {
        s.talk("hester"); s.choose("old song"); s.finish();
        assertTrue(s.state.flag("stones_awake"));
        assertTrue(s.hasItem("lullaby"));
    }

    static void meetWren(DialogueSim s) {
        s.talk("wren"); s.finish();
        s.talk("wren"); s.choose("Singing Stones"); s.finish();
        assertTrue(s.state.flag("wren_guided"));
    }

    /** Everything after the stones, driven by world flags where the game code would set them. */
    static void restOfMainStory(DialogueSim s, boolean tellWarden) {
        s.state.set("puzzle1_solved");
        assertTrue(s.done("m2")); assertTrue(s.active("m3"));
        s.state.set("found_log"); s.state.set("puzzle2_solved"); s.state.set("saw_empty_lantern"); s.give("warden_key");
        assertTrue(s.done("m3")); assertTrue(s.active("m4"));
        for (int i = 1; i <= 3; i++) { s.at.clear(); s.at.add("echo_" + i); s.talk("echo"); s.finish(); }
        s.at.clear();
        assertTrue(s.state.flag("gate.echo_door"), "three echoes open the way on");
        assertTrue(s.state.flag("knows_hester"));
        s.state.set("puzzle3_solved");
        assertTrue(s.active("m5"));
        s.state.set("boss_defeated");
        s.talk("warden"); s.next(); s.next();
        if (tellWarden) { s.choose("daughter"); s.next(); s.next(); s.next(); }
        else { s.choose("peace"); s.next(); }
        assertFalse(s.open());
        assertTrue(s.state.flag("warden_rest")); assertTrue(s.hasItem("ember"));
        assertEquals(tellWarden, s.state.flag("told_warden"));
        assertTrue(s.active("m6"));
        s.state.set("lantern_lit");
        assertTrue(s.active("m7"));
        s.state.set("festival");
    }

    static void finale(DialogueSim s) {
        s.talk("hester"); assertEquals("epi", s.node());
        s.next(); s.next(); s.next(); s.choose("pier now");
        assertTrue(s.state.flag("credits_seen"));
        assertTrue(s.done("m7"));
    }

    @Test void mainStoryWithoutAnyRomance() throws Exception {
        DialogueSim s = fresh();
        act1(s); learnSong(s); meetWren(s);
        s.talk("corin"); s.finish(); s.talk("corin"); s.choose("heading out"); s.finish();
        assertTrue(s.hasItem("smith_sword"));
        restOfMainStory(s, true);
        finale(s);
        assertEquals(0, s.var("dating"));
        for (String r : new String[]{"wren", "ione", "sol"}) assertFalse(s.state.flag(r + "_romance"));
    }

    @Test void mainStoryWhereWardenIsNotTold() throws Exception {
        DialogueSim s = fresh();
        act1(s); learnSong(s);
        restOfMainStory(s, false);
        finale(s);
    }

    // ------------------------------------------------------------ romances
    static void wrenToGirlfriend(DialogueSim s, boolean honestyPath) {
        if (!s.state.flag("met.wren")) meetWren(s);
        s.talk("wren"); s.choose("nicer than you pretend"); s.choose("Maybe you should"); s.finish();
        assertTrue(s.state.flag("wren_spark"));
        s.talk("wren"); s.choose("far away"); s.next(); s.choose("I'll get it"); s.finish();
        assertTrue(s.active("p_wren"));
        s.give("mother_bow");
        s.talk("wren"); s.choose("Hollow Oak"); s.next(); s.next(); s.choose("more than friends"); s.choose("I'll be there."); 
        assertTrue(s.state.flag("wren_date_set")); assertTrue(s.done("p_wren"));
        s.at.add("glade"); s.time = 14000;
        assertEquals("date", s.talk("wren"));
        s.next(); s.next(); s.next();
        if (honestyPath) { s.choose("already seeing someone"); s.next(); s.choose("knowing all of it"); }
        else { assertTrue(s.options().stream().noneMatch(o -> o.contains("already seeing"))); s.choose("will you be mine"); }
        s.finish();
        s.at.clear(); s.time = 6000;
        assertTrue(s.state.flag("wren_dating"));
    }

    static void ioneToGirlfriend(DialogueSim s, boolean honestyPath) {
        s.talk("ione"); s.choose("adorable"); s.next(); s.finish();
        if (s.var("rel.ione") < 2) { s.talk("ione"); s.choose("Read with me"); s.next(); s.finish(); }
        s.talk("ione"); s.choose("worried"); s.next(); s.choose("all three"); 
        s.give("page_1"); s.give("page_2"); s.give("page_3");
        assertEquals("pages_back", s.talk("ione"));
        s.next(); s.next(); s.next(); s.choose("more than friends"); s.choose("I'll be there.");
        assertTrue(s.state.flag("ione_date_set"));
        s.at.add("archive_roof"); s.time = 15000;
        assertEquals("date", s.talk("ione"));
        s.next(); s.next(); s.next();
        if (honestyPath) { s.choose("already with someone"); s.next(); s.choose("knowing all of it"); }
        else s.choose("love to be with you");
        s.finish();
        s.at.clear(); s.time = 6000;
        assertTrue(s.state.flag("ione_dating"));
    }

    static void solToGirlfriend(DialogueSim s, boolean honestyPath) {
        s.talk("sol"); s.finish();
        s.talk("sol"); s.choose("rig me"); s.next(); s.finish();
        s.talk("sol"); s.choose("leave so badly"); s.choose("I came from it"); s.finish();
        s.talk("sol"); s.choose("horizon"); s.next(); s.next(); s.choose("all three");
        s.give("brass_compass"); s.give("sailcloth"); s.give("mast_timber");
        assertEquals("parts_back", s.talk("sol"));
        s.next(); s.next(); s.choose("more than friends"); s.choose("It's a date.");
        assertTrue(s.state.flag("sol_date_set"));
        s.at.add("arrival"); s.time = 12000;
        assertEquals("date", s.talk("sol"));
        s.choose("Climb into the boat");
        assertTrue(s.state.flag("sol_sailing"));
        s.at.clear();
        assertEquals("rock", s.talk("sol"));
        s.next(); s.next();
        if (honestyPath) { s.choose("already with someone"); s.next(); s.choose("knowing all of it"); }
        else s.choose("I'm yours");
        s.next(); s.next();
        assertFalse(s.state.flag("sol_sailing"), "rowed home again");
        s.time = 6000;
        assertTrue(s.state.flag("sol_dating"));
    }

    @Test void datingOneCharacter() throws Exception {
        DialogueSim s = fresh();
        act1(s); learnSong(s);
        wrenToGirlfriend(s, false);
        assertEquals(1, s.var("dating"));
        assertFalse(s.state.flag("poly_open"));
        restOfMainStory(s, true);
        s.talk("wren"); assertEquals("epi", s.node()); s.choose("..."); assertEquals("epi_dating", s.node()); s.finish();
        finale(s);
    }

    @Test void datingTwoCharacters() throws Exception {
        DialogueSim s = fresh();
        act1(s); learnSong(s);
        ioneToGirlfriend(s, false);
        wrenToGirlfriend(s, true);
        assertEquals(2, s.var("dating"));
        assertTrue(s.state.flag("poly_open"));
        // Ione was dating first: she gets a scene confirming they all talked
        assertEquals("poly_ack", s.talk("ione")); s.next(); s.finish();
        assertTrue(s.state.flag("ione_poly_ack"));
        assertNotEquals("poly_ack", s.talk("wren")); s.finish();
    }

    @Test void datingAllThreeAtOnce() throws Exception {
        DialogueSim s = fresh();
        act1(s); learnSong(s);
        wrenToGirlfriend(s, false);
        ioneToGirlfriend(s, true);
        solToGirlfriend(s, true);
        assertEquals(3, s.var("dating"));
        assertEquals("poly_ack", s.talk("wren")); s.next(); s.finish();
        for (String r : new String[]{"wren", "ione", "sol"}) assertTrue(s.state.flag(r + "_dating"), r);
        restOfMainStory(s, true);
        assertEquals("epi_all", s.talk("sol"));
        s.finish();
        assertTrue(s.state.flag("all_three_scene"));
        for (String r : new String[]{"wren", "ione", "sol"}) assertTrue(s.state.flag(r + "_epi"));
        finale(s);
    }

    @Test void friendshipPathsWithoutRomance() throws Exception {
        DialogueSim s = fresh();
        act1(s); learnSong(s); meetWren(s);
        s.talk("wren"); s.choose("far away"); s.next(); s.choose("I'll get it"); s.finish();
        s.give("mother_bow");
        s.talk("wren"); s.choose("Hollow Oak"); s.next(); s.next();
        assertTrue(s.options().stream().noneMatch(o -> o.contains("more than friends")), "no romance option without a romantic spark");
        s.choose("best friends"); s.finish();
        assertTrue(s.state.flag("wren_bestfriend")); assertFalse(s.state.flag("wren_dating"));
        s.talk("sol"); s.finish(); s.talk("sol"); s.choose("leave so badly"); s.choose("I came from it"); s.finish();
        s.talk("sol"); s.choose("hull"); s.next(); s.finish();
        s.talk("sol"); s.choose("horizon"); s.next(); s.next(); s.choose("all three");
        s.give("brass_compass"); s.give("sailcloth"); s.give("mast_timber");
        s.talk("sol"); s.next(); s.next(); s.choose("best friends"); s.finish();
        assertTrue(s.state.flag("sol_bestfriend"));
        restOfMainStory(s, true);
        s.talk("wren"); s.choose("..."); assertEquals("epi_friend", s.node()); s.finish();
        assertEquals(0, s.var("dating"));
        finale(s);
    }

    @Test void sideQuestsComplete() throws Exception {
        DialogueSim s = fresh();
        act1(s);
        s.talk("corin"); s.finish(); s.talk("corin"); s.choose("forge seems cold"); s.next(); s.choose("armoury");
        s.give("keep_steel");
        s.talk("corin"); s.next(); s.next(); assertTrue(s.state.flag("side_corin_done")); assertTrue(s.hasItem("kestrel_blade"));
        s.talk("tobias"); s.choose("weighing"); s.next(); s.choose("find your log"); s.finish();
        s.talk("hester"); s.finish(); s.talk("hester"); s.choose("Diver's Draught"); s.finish();
        assertTrue(s.hasItem("diver_draught"));
        s.give("logbook");
        assertEquals("log_back", s.talk("tobias")); s.next(); s.next(); s.next();
        assertTrue(s.state.flag("side_tobias_done")); assertTrue(s.hasItem("spyglass"));
        s.talk("wendel"); s.finish(); s.talk("wendel"); s.choose("help with anything"); s.choose("light them"); s.finish();
        assertTrue(s.hasItem("tinderbox"));
        s.state.setVar("waystones_lit", 4);
        assertEquals("lamps_done", s.talk("wendel")); s.next(); s.next();
        assertTrue(s.state.flag("side_wendel_done")); assertTrue(s.hasItem("wendel_charm"));
        for (String q : new String[]{"s_corin", "s_tobias", "s_wendel"}) assertTrue(s.done(q), q);
    }

    @Test void stateSurvivesSaveAndLoad() throws Exception {
        DialogueSim s = fresh();
        act1(s); learnSong(s); wrenToGirlfriend(s, false);
        IslandState st = new IslandState();
        st.data.copyFrom(s.state);
        var lookup = BuiltinRegistries.createWrapperLookup();
        NbtCompound nbt = st.writeNbt(new NbtCompound(), lookup);
        IslandState back = IslandState.fromNbt(nbt, lookup);
        assertEquals(s.state.flags(), back.data.flags());
        assertEquals(s.state.vars(), back.data.vars());
        assertTrue(back.data.flag("wren_dating"));
        assertEquals(s.var("rel.wren"), back.data.var("rel.wren"));
    }
}
