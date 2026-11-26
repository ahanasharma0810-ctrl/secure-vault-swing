package com.securevault.ui;

import com.securevault.ui.components.SiteAvatar;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SiteAvatarTest {

    @Test
    void usesFirstLetterOrDigit() {
        assertEquals("G", SiteAvatar.initialFor("gmail.com"));
        assertEquals("9", SiteAvatar.initialFor("9gag.com"));
        assertEquals("B", SiteAvatar.initialFor("  @bank app"));
        assertEquals("?", SiteAvatar.initialFor("..."));
        assertEquals("?", SiteAvatar.initialFor(null));
    }

    @Test
    void sameSiteAlwaysGetsSameColour() {
        assertEquals(SiteAvatar.colorFor("gmail.com"), SiteAvatar.colorFor("GMAIL.com"));
        assertNotNull(SiteAvatar.colorFor(null));
    }
}
