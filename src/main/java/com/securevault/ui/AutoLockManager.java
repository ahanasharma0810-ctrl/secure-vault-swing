package com.securevault.ui;

import javax.swing.Timer;
import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;

/**
 * Locks the vault after a period with no mouse or keyboard activity.
 * Listens to every input event in the application and restarts a timer.
 */
public class AutoLockManager {

    private static final long INPUT_EVENTS = AWTEvent.KEY_EVENT_MASK
            | AWTEvent.MOUSE_EVENT_MASK
            | AWTEvent.MOUSE_MOTION_EVENT_MASK
            | AWTEvent.MOUSE_WHEEL_EVENT_MASK;

    private final Timer idleTimer;
    private final AWTEventListener activityListener;

    public AutoLockManager(int idleMinutes, Runnable onIdle) {
        idleTimer = new Timer(idleMinutes * 60 * 1000, e -> {
            stop();
            onIdle.run();
        });
        idleTimer.setRepeats(false);
        activityListener = event -> idleTimer.restart();
    }

    public void start() {
        Toolkit.getDefaultToolkit().addAWTEventListener(activityListener, INPUT_EVENTS);
        idleTimer.restart();
    }

    public void stop() {
        idleTimer.stop();
        Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener);
    }
}
