package ru.sanseddy.cctweakedunicodesupport.rom;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class RomOverlayMountTest {

    @Test
    void overlayTakesPrecedenceOverBase() throws IOException {
        var base = new ClasspathMount("data/computercraft/lua/rom")
            .addFile("apis/fs.lua");
        var overlay = new ClasspathMount("data/computercraft/lua/rom")
            .addFile("apis/window.lua");

        var combined = new RomOverlayMount(base, overlay);

        assertTrue(combined.exists("apis/window.lua"));
        assertTrue(combined.exists("apis/fs.lua"));
        assertFalse(combined.exists("apis/nonexistent.lua"));

        assertTrue(combined.isDirectory("apis"));
        assertFalse(combined.isDirectory("apis/window.lua"));

        var list = new ArrayList<String>();
        combined.list("apis", list);
        assertTrue(list.contains("window.lua"));
        assertTrue(list.contains("fs.lua"));

        assertTrue(combined.getSize("apis/window.lua") > 0);
        try (var channel = combined.openForRead("apis/window.lua")) {
            var buf = ByteBuffer.allocate((int) channel.size());
            channel.read(buf);
            var content = new String(buf.array(), StandardCharsets.UTF_8);
            assertTrue(content.contains("setGraphicsMode"));
            assertTrue(content.contains("text_sub"));
        }
    }
}
