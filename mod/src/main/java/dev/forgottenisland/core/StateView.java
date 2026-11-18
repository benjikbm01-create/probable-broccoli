package dev.forgottenisland.core;

/** Read access to game state used by conditions. Implemented by the live world state and by test fakes. */
public interface StateView {
    boolean flag(String name);
    int var(String name);
    /** Whether the player currently carries the named key item. */
    default boolean hasItem(String id) { return false; }
    /** Ticks into the current day (0 = 06:00, 6000 = noon, 13000 = night falls). */
    default long timeOfDay() { return 6000; }
}
