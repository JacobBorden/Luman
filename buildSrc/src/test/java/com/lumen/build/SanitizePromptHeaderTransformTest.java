package com.lumen.build;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import org.junit.Test;

public final class SanitizePromptHeaderTransformTest {
    private static final Method SANITIZE_METHOD;

    static {
        try {
            SANITIZE_METHOD =
                    SanitizePromptHeaderTransform.class.getDeclaredMethod(
                            "sanitizePlaceholders", String.class);
            SANITIZE_METHOD.setAccessible(true);
        } catch (NoSuchMethodException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static String sanitize(String content) {
        try {
            return (String) SANITIZE_METHOD.invoke(null, content);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new RuntimeException("Unable to invoke sanitizePlaceholders", exception);
        }
    }

    @Test
    public void sanitizePlaceholders_reusesIndexForRepeatedPlaceholder() {
        String sanitized = sanitize("{user} • {user}");
        assertEquals("%1$s • %1$s", sanitized);
        assertEquals("Ada • Ada", String.format(Locale.US, sanitized, "Ada"));
    }

    @Test
    public void sanitizePlaceholders_assignsSequentialIndicesToDistinctPlaceholders() {
        String sanitized = sanitize("{user}:{org}");
        assertEquals("%1$s:%2$s", sanitized);
    }

    @Test
    public void sanitizePlaceholders_ignoresWhitespaceDifferencesForRepeatedPlaceholder() {
        String sanitized = sanitize("{ user } vs {\tuser\n}");
        assertEquals("%1$s vs %1$s", sanitized);
    }
}
