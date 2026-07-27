package com.mj.sign;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ModelProtocolValidator {

    private ModelProtocolValidator() {
    }

    static List<String> validateResponse(GpuInferenceResponse response, InferenceContext context) {
        List<String> errors = new ArrayList<>();
        if (response == null) {
            errors.add("model response is empty");
            return errors;
        }

        requireMatch(errors, "protocol_version", response.protocol_version(), context.protocol_version());
        requireMatch(errors, "locale", response.locale(), context.locale());
        requireMatch(errors, "sign_language", response.sign_language(), context.sign_language());
        requireMatch(errors, "model_profile", response.model_profile(), context.model_profile());
        return List.copyOf(errors);
    }

    static double normalizedConfidence(Number confidence) {
        if (confidence == null) {
            return 0.0;
        }
        double value = confidence.doubleValue();
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    private static void requireMatch(
            List<String> errors,
            String field,
            String actual,
            String expected
    ) {
        if (isBlank(actual)) {
            return;
        }
        if (!normalize(actual).equals(normalize(expected))) {
            errors.add(field + " mismatch: expected " + expected + " but got " + actual);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
