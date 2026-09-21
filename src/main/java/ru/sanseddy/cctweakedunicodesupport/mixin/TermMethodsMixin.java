package ru.sanseddy.cctweakedunicodesupport.mixin;

import dan200.computercraft.api.lua.Coerced;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.TermMethods;
import dan200.computercraft.core.terminal.Terminal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.sanseddy.cctweakedunicodesupport.text.Utf8;

import java.nio.ByteBuffer;

@Mixin(TermMethods.class)
public abstract class TermMethodsMixin {
    @Shadow
    public abstract Terminal getTerminal() throws LuaException;

    @Inject(method = "write", at = @At("HEAD"), cancellable = true)
    private void cc_tweaked_unicode_support$writeDecoded(Coerced<String> textArg, CallbackInfo ci) throws LuaException {
        var text = Utf8.decode(textArg.value());
        var terminal = getTerminal();
        synchronized (terminal) {
            terminal.write(text);
            terminal.setCursorPos(terminal.getCursorX() + text.length(), terminal.getCursorY());
        }
        ci.cancel();
    }

    @Inject(method = "blit", at = @At("HEAD"), cancellable = true)
    private void cc_tweaked_unicode_support$blitDecoded(
        ByteBuffer text, ByteBuffer textColour, ByteBuffer backgroundColour, CallbackInfo ci
    ) throws LuaException {
        var cells = Utf8.decode(Utf8.asByteString(text));
        var terminal = getTerminal();
        var fgLen = textColour.remaining();
        var bgLen = backgroundColour.remaining();

        synchronized (terminal) {
            var x = terminal.getCursorX();
            var y = terminal.getCursorY();
            if (y >= 0 && y < terminal.getHeight()) {
                var line = terminal.getLine(y);
                var foreground = terminal.getTextColourLine(y);
                var background = terminal.getBackgroundColourLine(y);
                for (var i = 0; i < cells.length(); i++) {
                    line.setChar(x + i, cells.charAt(i));
                    char fgChar = fgLen > 0 ? (char) (textColour.get(textColour.position() + Math.max(0, Math.min(i, fgLen - 1))) & 0xFF) : '0';
                    char bgChar = bgLen > 0 ? (char) (backgroundColour.get(backgroundColour.position() + Math.max(0, Math.min(i, bgLen - 1))) & 0xFF) : 'f';
                    foreground.setChar(x + i, fgChar);
                    background.setChar(x + i, bgChar);
                }
                terminal.setChanged();
            }

            terminal.setCursorPos(x + cells.length(), y);
        }
        ci.cancel();
    }
}
