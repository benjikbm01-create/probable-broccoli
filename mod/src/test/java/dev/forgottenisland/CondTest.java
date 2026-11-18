package dev.forgottenisland;

import dev.forgottenisland.core.Cond;
import dev.forgottenisland.core.StateData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CondTest {
    @Test void flagsAndOperators() {
        StateData s = new StateData();
        s.set("a"); s.setVar("n", 3);
        assertTrue(Cond.test("a", s));
        assertFalse(Cond.test("b", s));
        assertTrue(Cond.test("a && !b", s));
        assertTrue(Cond.test("b || a", s));
        assertFalse(Cond.test("!(a || b)", s));
        assertTrue(Cond.test("n >= 3 && n < 4 && n == 3 && n != 2 && n > 2 && n <= 3", s));
        assertTrue(Cond.test("", s));
        assertTrue(Cond.test(null, s));
        assertTrue(Cond.test("(a && (n > 1)) || zzz", s));
    }

    @Test void computedVariables() {
        StateData s = new StateData();
        assertEquals(0, s.var("dating"));
        s.set("wren_dating"); s.set("sol_dating");
        assertEquals(2, s.var("dating"));
        s.set("secret.a"); s.set("secret.b");
        assertEquals(2, s.var("secrets"));
    }

    @Test void relationshipsClamp() {
        StateData s = new StateData();
        s.addVar("rel.wren", 50);
        assertEquals(10, s.var("rel.wren"));
        s.addVar("rel.wren", -99);
        assertEquals(0, s.var("rel.wren"));
    }

    @Test void resetKeepsDevMode() {
        StateData s = new StateData();
        s.set("dev_mode"); s.set("x"); s.setVar("y", 2);
        s.reset();
        assertTrue(s.flag("dev_mode"));
        assertFalse(s.flag("x"));
        assertEquals(0, s.var("y"));
    }

    @Test void badSyntaxIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Cond.compile("a &&"));
        assertThrows(IllegalArgumentException.class, () -> Cond.compile("(a"));
        assertThrows(IllegalArgumentException.class, () -> Cond.compile("n >= x"));
    }

    @Test void timeConditions() {
        StateData s = new StateData() {};
        var day = new dev.forgottenisland.core.StateView() { public boolean flag(String n) { return false; } public int var(String n) { return 0; } public long timeOfDay() { return 6000; } };
        var night = new dev.forgottenisland.core.StateView() { public boolean flag(String n) { return false; } public int var(String n) { return 0; } public long timeOfDay() { return 18000 + 24000 * 5; } };
        assertTrue(Cond.test("time:day", day)); assertFalse(Cond.test("time:night", day));
        assertTrue(Cond.test("time:night", night)); assertFalse(Cond.test("time:day", night));
        assertNotNull(s);
    }
}
