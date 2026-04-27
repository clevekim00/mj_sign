package com.mj.sign;

import org.springframework.stereotype.Service;

import java.util.Locale;

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
        String signLanguage = normalizeToken(
                rawSignLanguage,
                properties.getSignLanguageByLocaleLanguage()
                        .getOrDefault(language, properties.getDefaultSignLanguage())
        );
        String modelProfile = normalizeToken(
                rawModelProfile,
                properties.getModelProfileBySignLanguage()
                        .getOrDefault(signLanguage, properties.getDefaultModelProfile())
        );

        return new InferenceContext(
                locale,
                signLanguage,
                modelProfile,
                normalizeToken(
                        rawProtocolVersion,
                        normalizeToken(properties.getProtocolVersion(), InferenceContext.DEFAULT_PROTOCOL_VERSION)
                )
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
}
