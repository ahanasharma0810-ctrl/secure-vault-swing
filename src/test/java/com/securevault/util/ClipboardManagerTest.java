package com.securevault.util;

import org.junit.jupiter.api.Test;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;

import static org.junit.jupiter.api.Assertions.*;

class ClipboardManagerTest {

    private static String read(Clipboard c) throws Exception {
        return (String) c.getData(DataFlavor.stringFlavor);
    }

    @Test
    void clearsWhatItCopied() throws Exception {
        Clipboard clipboard = new Clipboard("test");
        ClipboardManager manager = new ClipboardManager(clipboard, 60);
        manager.copy("secret");
        assertEquals("secret", read(clipboard));
        assertTrue(manager.isClearPending());

        manager.clearIfUnchanged();
        assertEquals("", read(clipboard));
        assertFalse(manager.isClearPending());
    }

    @Test
    void leavesOtherContentAlone() throws Exception {
        Clipboard clipboard = new Clipboard("test");
        ClipboardManager manager = new ClipboardManager(clipboard, 60);
        manager.copy("secret");
        clipboard.setContents(new StringSelection("something else"), null);   // user copied something new
        manager.clearIfUnchanged();
        assertEquals("something else", read(clipboard));
    }

    @Test
    void clearsAutomaticallyAfterTimeout() throws Exception {
        Clipboard clipboard = new Clipboard("test");
        ClipboardManager manager = new ClipboardManager(clipboard, 1);
        manager.copy("secret");
        Thread.sleep(1800);
        assertEquals("", read(clipboard));
    }
}
