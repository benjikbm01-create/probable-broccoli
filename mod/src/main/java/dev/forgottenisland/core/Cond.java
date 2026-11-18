package dev.forgottenisland.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Tiny condition language used by dialogue, quests and schedules.
 * <pre>
 *   flag_name            true when the flag is set
 *   !expr  a && b  a || b  ( expr )
 *   var >= 3             comparison on an integer variable (==, !=, >=, <=, >, <)
 *   has:item_id          player carries the key item
 *   time:day | time:night | time:evening | time:morning
 *   true / false
 * </pre>
 * Computed variables: {@code dating} (number of current relationships), {@code secrets} (secrets found).
 */
public final class Cond {
    private static final Map<String, Predicate<StateView>> CACHE = new ConcurrentHashMap<>();
    private Cond() {}

    public static boolean test(String expr, StateView s) {
        if (expr == null || expr.isBlank()) return true;
        return compile(expr).test(s);
    }

    public static Predicate<StateView> compile(String expr) {
        return CACHE.computeIfAbsent(expr, e -> new Parser(tokenize(e), e).parse());
    }

    /** Returns every flag/variable name referenced by an expression (used by content validation). */
    public static List<String> names(String expr) {
        List<String> out = new ArrayList<>();
        if (expr == null) return out;
        for (String t : tokenize(expr)) if (isIdent(t) && !t.equals("true") && !t.equals("false") && !Character.isDigit(t.charAt(0)) && !t.startsWith("-")) out.add(t);
        return out;
    }

    static boolean isIdent(String t) {
        char c = t.charAt(0);
        return Character.isLetterOrDigit(c) || c == '_' || c == '-';
    }

    static List<String> tokenize(String s) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (c == '(' || c == ')') { out.add(String.valueOf(c)); i++; continue; }
            if (s.startsWith("&&", i) || s.startsWith("||", i) || s.startsWith(">=", i) || s.startsWith("<=", i) || s.startsWith("==", i) || s.startsWith("!=", i)) {
                out.add(s.substring(i, i + 2)); i += 2; continue;
            }
            if (c == '!' || c == '>' || c == '<') { out.add(String.valueOf(c)); i++; continue; }
            int j = i;
            while (j < s.length() && (Character.isLetterOrDigit(s.charAt(j)) || "_.:-".indexOf(s.charAt(j)) >= 0)) j++;
            if (j == i) throw new IllegalArgumentException("Bad character '" + c + "' in condition: " + s);
            out.add(s.substring(i, j)); i = j;
        }
        return out;
    }

    private static final class Parser {
        final List<String> t; final String src; int p;
        Parser(List<String> t, String src) { this.t = t; this.src = src; }
        String peek() { return p < t.size() ? t.get(p) : null; }
        String next() { if (p >= t.size()) throw err("unexpected end"); return t.get(p++); }
        IllegalArgumentException err(String m) { return new IllegalArgumentException(m + " in condition: " + src); }

        Predicate<StateView> parse() {
            Predicate<StateView> r = or();
            if (p != t.size()) throw err("unexpected '" + peek() + "'");
            return r;
        }
        Predicate<StateView> or() {
            Predicate<StateView> l = and();
            while ("||".equals(peek())) { next(); Predicate<StateView> a = l, b = and(); l = s -> a.test(s) || b.test(s); }
            return l;
        }
        Predicate<StateView> and() {
            Predicate<StateView> l = unary();
            while ("&&".equals(peek())) { next(); Predicate<StateView> a = l, b = unary(); l = s -> a.test(s) && b.test(s); }
            return l;
        }
        Predicate<StateView> unary() {
            String k = peek();
            if ("!".equals(k)) { next(); Predicate<StateView> in = unary(); return s -> !in.test(s); }
            if ("(".equals(k)) { next(); Predicate<StateView> in = or(); if (!")".equals(next())) throw err("missing )"); return in; }
            return atom();
        }
        Predicate<StateView> atom() {
            String name = next();
            if (!isIdent(name)) throw err("expected name, got '" + name + "'");
            String op = peek();
            if (op != null && (op.equals("==") || op.equals("!=") || op.equals(">=") || op.equals("<=") || op.equals(">") || op.equals("<"))) {
                next();
                String num = next();
                int n;
                try { n = Integer.parseInt(num); } catch (NumberFormatException e) { throw err("expected number after " + op); }
                return switch (op) {
                    case "==" -> s -> s.var(name) == n;
                    case "!=" -> s -> s.var(name) != n;
                    case ">=" -> s -> s.var(name) >= n;
                    case "<=" -> s -> s.var(name) <= n;
                    case ">" -> s -> s.var(name) > n;
                    default -> s -> s.var(name) < n;
                };
            }
            if (name.equals("true")) return s -> true;
            if (name.equals("false")) return s -> false;
            if (name.startsWith("has:")) { String id = name.substring(4); return s -> s.hasItem(id); }
            if (name.startsWith("time:")) {
                String w = name.substring(5);
                return switch (w) {
                    case "day" -> s -> s.timeOfDay() % 24000 < 12500;
                    case "night" -> s -> s.timeOfDay() % 24000 >= 13000 && s.timeOfDay() % 24000 < 23000;
                    case "evening" -> s -> s.timeOfDay() % 24000 >= 11000 && s.timeOfDay() % 24000 < 14500;
                    case "morning" -> s -> s.timeOfDay() % 24000 >= 23000 || s.timeOfDay() % 24000 < 4000;
                    default -> throw err("unknown time '" + w + "'");
                };
            }
            return s -> s.flag(name);
        }
    }
}
