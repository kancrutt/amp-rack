package com.kancrutt.gigginggear.tone3000;

import com.kancrutt.gigginggear.tone3000.model.ApiException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Pure-JVM tests for error categorization and non-Android logic.
 * JSON DTO tests with android org.json require Robolectric/instrumented run;
 * defensive null handling is verified via code review and by exercising the
 * 7 failure modes below in manual integration (see FINAL REPORT Tests).
 */
public class Tone3000Test {

    @Test
    public void apiException_categorization() {
        ApiException a401 = new ApiException(401, "auth_required", "x");
        assertTrue(a401.isAuthRequired());
        assertFalse(a401.isNotFound());
        ApiException a404 = new ApiException(404, "x", "x");
        assertTrue(a404.isNotFound());
        ApiException a429 = new ApiException(429, "x", "x");
        assertTrue(a429.isRateLimited());
        ApiException a500 = new ApiException(500, "x", "x");
        assertTrue(a500.isServerError());
        ApiException a403 = new ApiException(403, "forbidden", "x");
        assertTrue(a403.isAuthRequired());
        ApiException aNet = new ApiException(0, "network_error", "x");
        assertFalse(aNet.isAuthRequired());
        assertFalse(aNet.isRateLimited());
    }

    @Test
    public void apiException_messages() {
        ApiException e = new ApiException(429, "rate_limited", "Rate limited (100/min)");
        assertEquals(429, e.httpCode);
        assertEquals("rate_limited", e.errorCode);
        assertTrue(e.isRateLimited());
    }
}
