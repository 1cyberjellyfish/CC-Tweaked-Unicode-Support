package ru.sanseddy.cctweakedunicodesupport.compat;

import dan200.computercraft.shared.computer.terminal.NetworkedTerminal;
import dan200.computercraft.shared.computer.terminal.TerminalState;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

/**
 * Safe reflection-based bridge for CC: Graphics compatibility without hard compile-time dependency.
 */
public final class CCGraphicsCompat {
    private static Method onReadMethod;
    private static boolean initialized = false;

    private static synchronized Method getOnReadMethod() {
        if (!initialized) {
            initialized = true;
            try {
                for (var m : NetworkedTerminal.class.getDeclaredMethods()) {
                    if (m.getName().contains("ccgraphics$onRead") && m.getParameterCount() == 2) {
                        m.setAccessible(true);
                        onReadMethod = m;
                        break;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return onReadMethod;
    }

    private CCGraphicsCompat() {
    }

    public static boolean isPresent() {
        return getOnReadMethod() != null;
    }

    public static void onNetworkedTerminalRead(NetworkedTerminal terminal, TerminalState state, CallbackInfo ci) {
        var m = getOnReadMethod();
        if (m != null) {
            try {
                m.invoke(terminal, state, ci);
            } catch (Throwable ignored) {
            }
        }
    }
}
