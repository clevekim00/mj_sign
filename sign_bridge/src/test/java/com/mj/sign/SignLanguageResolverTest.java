package com.mj.sign;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SignLanguageResolverTest {

    @Test
    void resolvesEnglishLocaleToAslAndSignGemma() {
        SignLanguageResolver resolver = new SignLanguageResolver(new SignLanguageProperties());

        InferenceContext context = resolver.resolve("en-US", null, null);

        assertEquals("en-US", context.locale());
        assertEquals("asl", context.sign_language());
        assertEquals("sign-gemma", context.model_profile());
        assertEquals("mj-sign-model-v1", context.protocol_version());
    }

    @Test
    void usesExplicitSignLanguageAndModelProfileWhenProvided() {
        SignLanguageResolver resolver = new SignLanguageResolver(new SignLanguageProperties());

        InferenceContext context = resolver.resolve("ko-KR", "asl", "custom-english-model", "mj-sign-model-v2");

        assertEquals("ko-KR", context.locale());
        assertEquals("asl", context.sign_language());
        assertEquals("custom-english-model", context.model_profile());
        assertEquals("mj-sign-model-v2", context.protocol_version());
    }
}
