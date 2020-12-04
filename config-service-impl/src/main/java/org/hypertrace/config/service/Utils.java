package org.hypertrace.config.service;

import java.util.Optional;

public class Utils {

    // prevents instantiation of this class
    private Utils() {}

    private static final String DEFAULT_CONTEXT = "DEFAULT";

    public static String optionalContextToString(Optional<String> context) {
        return context.isEmpty() ? DEFAULT_CONTEXT : context.get();
    }
}
