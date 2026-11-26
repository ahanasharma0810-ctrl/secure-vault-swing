package com.securevault.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShakeAnimatorTest {

    @Test
    void shakeEndsWhereItStarted() {
        int[] offsets = ShakeAnimator.OFFSETS;
        assertEquals(0, offsets[offsets.length - 1]);
    }

    @Test
    void shakeGetsSmaller() {
        int[] offsets = ShakeAnimator.OFFSETS;
        for (int i = 2; i < offsets.length; i++) {
            assertTrue(Math.abs(offsets[i]) <= Math.abs(offsets[i - 2]));
        }
    }
}
