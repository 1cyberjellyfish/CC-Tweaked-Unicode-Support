package ru.sanseddy.cctweakedunicodesupport.text;

import dan200.computercraft.core.util.StringUtil;

import java.util.Arrays;

public final class CraftOsCharset {

    public static final int SIZE = 256;

    public static final char LEGACY_ALIAS_BASE = '\uFDD0';

    public static final char LEGACY_ALIAS_END = '\uFDEE';

    public static final char CONTINUATION = '\uFDEF';

    public static final char ASTRAL_ALIAS_BASE = '\uE000';

    public static final int MAX_ASTRAL_ALIASES = 2048;

    private static final int[] TO_CODEPOINT = new int[SIZE];

    private static final char[] TO_CELL = new char[SIZE];

    private static final int[] ALIAS_TO_BYTE = new int[LEGACY_ALIAS_END - LEGACY_ALIAS_BASE + 1];

    private static final int[] ASTRAL_TO_CODEPOINT = new int[MAX_ASTRAL_ALIASES];

    private static final java.util.Map<Integer, Character> CODEPOINT_TO_ASTRAL = new java.util.concurrent.ConcurrentHashMap<>();

    private static final java.util.concurrent.atomic.AtomicInteger ASTRAL_COUNT = new java.util.concurrent.atomic.AtomicInteger(0);

    private static final java.nio.file.Path PERSISTENCE_PATH = java.nio.file.Paths.get("config", "cc_tweaked_unicode_astral.dat");

    static {
        Arrays.fill(TO_CODEPOINT, -1);

        for (var codepoint = 0; codepoint <= 0xFFFF; codepoint++) recordCodepoint(codepoint);
        for (var codepoint = 0x1FB00; codepoint <= 0x1FBFF; codepoint++) recordCodepoint(codepoint);

        var alias = 0;
        for (var b = 0; b < SIZE; b++) {
            var codepoint = TO_CODEPOINT[b];
            if (codepoint < 0) {
                TO_CELL[b] = (char) b;
            } else if (codepoint <= 0xFFFF) {
                TO_CELL[b] = (char) codepoint;
            } else {
                if (alias >= ALIAS_TO_BYTE.length) throw new IllegalStateException("Too many terminal aliases");
                TO_CELL[b] = (char) (LEGACY_ALIAS_BASE + alias);
                ALIAS_TO_BYTE[alias++] = b;
            }
        }
        if (alias != ALIAS_TO_BYTE.length) throw new IllegalStateException("Unexpected terminal alias count");

        loadPersistedAstralAliases();
    }

    private static void loadPersistedAstralAliases() {
        try {
            if (!java.nio.file.Files.exists(PERSISTENCE_PATH)) return;
            try (var in = new java.io.DataInputStream(new java.io.BufferedInputStream(java.nio.file.Files.newInputStream(PERSISTENCE_PATH)))) {
                int maxIdx = -1;
                while (in.available() > 0) {
                    int idx = in.readInt();
                    int codepoint = in.readInt();
                    if (idx >= 0 && idx < MAX_ASTRAL_ALIASES) {
                        ASTRAL_TO_CODEPOINT[idx] = codepoint;
                        CODEPOINT_TO_ASTRAL.put(codepoint, (char) (ASTRAL_ALIAS_BASE + idx));
                        if (idx > maxIdx) maxIdx = idx;
                    }
                }
                if (maxIdx >= 0) {
                    ASTRAL_COUNT.set(maxIdx + 1);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void appendPersistedAstralAlias(int idx, int codepoint) {
        try {
            var parent = PERSISTENCE_PATH.getParent();
            if (parent != null) {
                java.nio.file.Files.createDirectories(parent);
            }
            try (var out = new java.io.DataOutputStream(new java.io.BufferedOutputStream(
                java.nio.file.Files.newOutputStream(PERSISTENCE_PATH, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)))) {
                out.writeInt(idx);
                out.writeInt(codepoint);
            }
        } catch (Throwable ignored) {
        }
    }

    private CraftOsCharset() {
    }

    private static void recordCodepoint(int codepoint) {
        var b = StringUtil.unicodeToTerminal(codepoint);
        if (b >= 0 && b < SIZE && TO_CODEPOINT[b] < 0) TO_CODEPOINT[b] = codepoint;
    }

    public static int toCodepoint(int b) {
        return TO_CODEPOINT[b & 0xFF];
    }

    public static char toCell(int b) {
        return TO_CELL[b & 0xFF];
    }

    public static int toLegacyByte(int codepoint) {
        if (isInternalMarker(codepoint)) return -1;
        if (codepoint >= 0 && codepoint < SIZE && TO_CODEPOINT[codepoint] < 0
            && TO_CELL[codepoint] == codepoint) {
            return codepoint;
        }
        var b = StringUtil.unicodeToTerminal(codepoint);
        return b >= 0 && b < SIZE ? b : -1;
    }

    public static boolean isLegacy(int codepoint) {
        return toLegacyByte(codepoint) >= 0;
    }

    public static char toAstralCell(int codepoint) {
        Character cached = CODEPOINT_TO_ASTRAL.get(codepoint);
        if (cached != null) return cached;
        synchronized (ASTRAL_TO_CODEPOINT) {
            cached = CODEPOINT_TO_ASTRAL.get(codepoint);
            if (cached != null) return cached;
            int idx = ASTRAL_COUNT.getAndIncrement() % MAX_ASTRAL_ALIASES;
            int oldCodepoint = ASTRAL_TO_CODEPOINT[idx];
            if (oldCodepoint > 0) {
                CODEPOINT_TO_ASTRAL.remove(oldCodepoint);
            }
            char cell = (char) (ASTRAL_ALIAS_BASE + idx);
            ASTRAL_TO_CODEPOINT[idx] = codepoint;
            CODEPOINT_TO_ASTRAL.put(codepoint, cell);
            appendPersistedAstralAlias(idx, codepoint);
            return cell;
        }
    }

    public static int fromAstralCell(char cell) {
        if (cell >= ASTRAL_ALIAS_BASE && cell < ASTRAL_ALIAS_BASE + MAX_ASTRAL_ALIASES) {
            return ASTRAL_TO_CODEPOINT[cell - ASTRAL_ALIAS_BASE];
        }
        return -1;
    }

    public static int cellToCodepoint(char cell) {
        int astral = fromAstralCell(cell);
        if (astral > 0) return astral;
        if (cell >= LEGACY_ALIAS_BASE && cell <= LEGACY_ALIAS_END) {
            return ALIAS_TO_BYTE[cell - LEGACY_ALIAS_BASE];
        }
        return cell;
    }

    public static boolean isInternalMarker(int codepoint) {
        return (codepoint >= LEGACY_ALIAS_BASE && codepoint <= CONTINUATION)
            || (codepoint >= ASTRAL_ALIAS_BASE && codepoint < ASTRAL_ALIAS_BASE + MAX_ASTRAL_ALIASES);
    }

    public static int terminalOnlyGlyph(int codepoint) {
        if (codepoint >= LEGACY_ALIAS_BASE && codepoint <= LEGACY_ALIAS_END) {
            return ALIAS_TO_BYTE[codepoint - LEGACY_ALIAS_BASE];
        }
        if (codepoint == '\t' || codepoint == '\n' || codepoint == '\r') return codepoint;
        if (codepoint >= 0 && codepoint < SIZE && TO_CODEPOINT[codepoint] < 0
            && TO_CELL[codepoint] == codepoint) {
            return codepoint;
        }
        if (codepoint > 0xFFFF) {
            var legacy = toLegacyByte(codepoint);
            if (legacy >= 0 && TO_CODEPOINT[legacy] == codepoint) return legacy;
        }
        return -1;
    }
}
