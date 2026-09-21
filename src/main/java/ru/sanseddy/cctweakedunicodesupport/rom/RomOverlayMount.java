package ru.sanseddy.cctweakedunicodesupport.rom;

import dan200.computercraft.api.filesystem.Mount;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A read-only mount that overlays one mount on top of another.
 * Files in the overlay take precedence over the base mount.
 */
public class RomOverlayMount implements Mount {
    private final Mount base;
    private final Mount overlay;

    public RomOverlayMount(Mount base, Mount overlay) {
        this.base = base;
        this.overlay = overlay;
    }

    public Mount getBase() {
        return base;
    }

    public Mount getOverlay() {
        return overlay;
    }

    @Override
    public boolean exists(String path) throws IOException {
        return overlay.exists(path) || base.exists(path);
    }

    @Override
    public boolean isDirectory(String path) throws IOException {
        if (overlay.exists(path)) {
            if (overlay.isDirectory(path)) return true;
            return false;
        }
        return base.exists(path) && base.isDirectory(path);
    }

    @Override
    public void list(String path, List<String> contents) throws IOException {
        var listed = new ArrayList<String>();

        if (base.exists(path) && base.isDirectory(path)) base.list(path, listed);
        if (overlay.exists(path) && overlay.isDirectory(path)) overlay.list(path, listed);

        var seen = new HashSet<String>();
        for (var entry : listed) {
            if (seen.add(entry)) contents.add(entry);
        }
    }

    @Override
    public long getSize(String path) throws IOException {
        if (overlay.exists(path)) return overlay.getSize(path);
        return base.getSize(path);
    }

    @Override
    public SeekableByteChannel openForRead(String path) throws IOException {
        if (overlay.exists(path)) return overlay.openForRead(path);
        return base.openForRead(path);
    }

    @Override
    public BasicFileAttributes getAttributes(String path) throws IOException {
        if (overlay.exists(path)) return overlay.getAttributes(path);
        return base.getAttributes(path);
    }
}
