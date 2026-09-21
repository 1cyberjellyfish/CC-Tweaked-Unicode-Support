package ru.sanseddy.cctweakedunicodesupport.compat;

import net.minecraft.world.inventory.AbstractContainerMenu;

import java.lang.reflect.Method;

/**
 * Safe reflection-based bridge for CC: Terminals compatibility.
 */
public final class CCTerminalsCompat {
    private static final Method HANDLE_INPUT_METHOD;
    private static final Object CHAR_INPUT_TYPE;
    private static final Method BLOCK_ENTITY_HANDLE_INPUT;
    private static final java.lang.reflect.Field BLOCK_ENTITY_FIELD;

    static {
        Method m = null;
        Object charType = null;
        Method beHandle = null;
        java.lang.reflect.Field beField = null;
        try {
            Class<?> menuClass = Class.forName("com.sashafiesta.ccterminals.TerminalMenu");
            Class<?> inputTypeClass = Class.forName("com.sashafiesta.ccterminals.network.InputType");
            Class<?> beClass = Class.forName("com.sashafiesta.ccterminals.TerminalBlockEntity");
            for (Object constant : inputTypeClass.getEnumConstants()) {
                if ("CHAR".equals(((Enum<?>) constant).name())) {
                    charType = constant;
                    break;
                }
            }
            m = menuClass.getMethod("handleInput", inputTypeClass, int.class, int.class, int.class, String.class);
            m.setAccessible(true);
            beField = menuClass.getDeclaredField("blockEntity");
            beField.setAccessible(true);
            beHandle = beClass.getMethod("handleInput", inputTypeClass, int.class, int.class, int.class, String.class);
            beHandle.setAccessible(true);
        } catch (Throwable ignored) {
        }
        HANDLE_INPUT_METHOD = m;
        CHAR_INPUT_TYPE = charType;
        BLOCK_ENTITY_FIELD = beField;
        BLOCK_ENTITY_HANDLE_INPUT = beHandle;
    }

    private CCTerminalsCompat() {
    }

    public static boolean tryHandleChar(AbstractContainerMenu menu, int codepoint) {
        if (CHAR_INPUT_TYPE != null && menu != null
            && menu.getClass().getName().equals("com.sashafiesta.ccterminals.TerminalMenu")) {
            try {
                if (BLOCK_ENTITY_FIELD != null && BLOCK_ENTITY_HANDLE_INPUT != null) {
                    var be = BLOCK_ENTITY_FIELD.get(menu);
                    if (be != null) {
                        BLOCK_ENTITY_HANDLE_INPUT.invoke(be, CHAR_INPUT_TYPE, codepoint, 0, 0, "");
                        return true;
                    }
                }
                if (HANDLE_INPUT_METHOD != null) {
                    HANDLE_INPUT_METHOD.invoke(menu, CHAR_INPUT_TYPE, codepoint, 0, 0, "");
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }
}
