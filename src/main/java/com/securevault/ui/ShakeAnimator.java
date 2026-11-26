package com.securevault.ui;

import javax.swing.Timer;
import java.awt.Point;
import java.awt.Window;

/**
 * Shakes a window left and right for a moment - the classic
 * "wrong password" signal. The window always ends where it started.
 */
public final class ShakeAnimator {

    /** Horizontal offsets in pixels for each animation frame. */
    static final int[] OFFSETS = {-10, 10, -8, 8, -5, 5, -2, 2, 0};
    private static final int FRAME_MS = 30;

    private ShakeAnimator() {
    }

    public static void shake(Window window) {
        Point start = window.getLocation();
        final int[] frame = {0};
        Timer timer = new Timer(FRAME_MS, null);
        timer.addActionListener(e -> {
            if (frame[0] >= OFFSETS.length || !window.isShowing()) {
                timer.stop();
                window.setLocation(start);
                return;
            }
            window.setLocation(start.x + OFFSETS[frame[0]], start.y);
            frame[0]++;
        });
        timer.start();
    }
}
