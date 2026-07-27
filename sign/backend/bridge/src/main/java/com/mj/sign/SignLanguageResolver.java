package com.mj.sign;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
public class SignLanguageResolver {

    private final SignLanguageProperties properties;

    public SignLanguageResolver(SignLanguageProperties properties) {
        this.properties = properties;
    }

    public InferenceContext defaults() {
        return new InferenceContext(
                normalizeLocale(properties.getDefaultLocale(), InferenceContext.defaults().locale()),
                normalizeToken(properties.getDefaultSignLanguage(), InferenceContext.defaults().sign_language()),
                normalizeToken(properties.getDefaultModelProfile(), InferenceContext.defaults().model_profile()),
                normalizeToken(properties.getProtocolVersion(), InferenceContext.DEFAULT_PROTOCOL_VERSION)
        );
    }

    public InferenceContext resolve(String rawLocale, String rawSignLanguage, String rawModelProfile) {
        return resolve(rawLocale, rawSignLanguage, rawModelProfile, null);
    }

    public InferenceContext resolve(
            String rawLocale,
            String rawSignLanguage,
            String rawModelProfile,
            String rawProtocolVersion
    ) {
        String locale = normalizeLocale(rawLocale, properties.getDefaultLocale());
        String language = Locale.forLanguageTag(locale).getLanguage().toLowerCase(Locale.ROOT);
        Map<String, String> modelProfiles = properties.getModelProfileBySignLanguage();
        String signLanguage = normalizeToken(
                rawSignLanguage,
                properties.getSignLanguageByLocaleLanguage()
                        .getOrDefault(language, properties.getDefaultSignLanguage())
        );
        if (hasText(rawSignLanguage) && !modelProfiles.containsKey(signLanguage)) {
            throw new IllegalArgumentException("unsupported sign_language: " + signLanguage);
        }
        String modelProfile = normalizeToken(
                rawModelProfile,
                modelProfiles.getOrDefault(signLanguage, properties.getDefaultModelProfile())
        );
        String expectedModelProfile = modelProfiles.get(signLanguage);
        if (hasText(rawModelProfile) && !modelProfiles.containsValue(modelProfile)) {
            throw new IllegalArgumentException("unsupported model_profile: " + modelProfile);
        }
        if (expectedModelProfile != null && !expectedModelProfile.equals(modelProfile)) {
            throw new IllegalArgumentException(
                    "model_profile " + modelProfile + " is not registered for sign_language " + signLanguage
            );
        }

        return new InferenceContext(
                locale,
                signLanguage,
                modelProfile,
                InferenceContext.normalizeProtocolVersion(normalizeToken(
                        rawProtocolVersion,
                        normalizeToken(properties.getProtocolVersion(), InferenceContext.DEFAULT_PROTOCOL_VERSION)
                ))
        );
    }

    private String normalizeLocale(String rawLocale, String fallback) {
        String candidate = normalizeToken(rawLocale, fallback).replace('_', '-');
        Locale locale = Locale.forLanguageTag(candidate);
        if (locale.getLanguage().isBlank()) {
            locale = Locale.forLanguageTag(fallback.replace('_', '-'));
        }
        return locale.toLanguageTag();
    }

    private String normalizeToken(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
