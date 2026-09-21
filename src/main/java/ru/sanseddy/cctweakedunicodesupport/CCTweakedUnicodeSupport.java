package ru.sanseddy.cctweakedunicodesupport;

import dan200.computercraft.shared.computer.menu.ComputerMenu;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.OperationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.sanseddy.cctweakedunicodesupport.network.WideCharPayload;
import ru.sanseddy.cctweakedunicodesupport.text.Utf8;

@Mod(CCTweakedUnicodeSupport.MOD_ID)
public class CCTweakedUnicodeSupport {
    public static final String MOD_ID = "cc_tweaked_unicode_support";

    private static final Logger LOGGER = LoggerFactory.getLogger(CCTweakedUnicodeSupport.class);

    public CCTweakedUnicodeSupport(IEventBus modEventBus) {
        verifyUnicodeLengthOperator();
        LOGGER.info("CC: Tweaked Unicode terminal support loaded");
        modEventBus.addListener(CCTweakedUnicodeSupport::registerPayloads);
    }

    private static void verifyUnicodeLengthOperator() {
        try {
            var cyrillicLetter = LuaString.valueOf(new byte[]{(byte) 0xD0, (byte) 0xAF});
            var actual = OperationHelper.length(new LuaState(), cyrillicLetter).checkInteger();
            if (actual != 1) {
                LOGGER.warn("Cobalt's # operator counts UTF-8 bytes (expected 1, got {}). Falling back to runtime unicode helpers.", actual);
                return;
            }
            LOGGER.info("Unicode-aware Lua # operator enabled");
        } catch (Throwable failure) {
            LOGGER.warn("Could not verify Cobalt's Unicode length operator ({}). Continuing with standard Lua runtime.", failure.getMessage());
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {

        event.registrar("2").playToServer(WideCharPayload.TYPE, WideCharPayload.STREAM_CODEC, (payload, context) ->
            context.enqueueWork(() -> {
                if (!payload.isValid()) return;
                var player = context.player();
                if (player.containerMenu.containerId == payload.containerId()) {
                    if (player.containerMenu instanceof ComputerMenu menu) {
                        menu.getComputer().queueEvent("char", new Object[]{Utf8.encodePreferLegacy(payload.codepoint())});
                    } else {
                        ru.sanseddy.cctweakedunicodesupport.compat.CCTerminalsCompat.tryHandleChar(player.containerMenu, payload.codepoint());
                    }
                }
            })
        );
    }
}
