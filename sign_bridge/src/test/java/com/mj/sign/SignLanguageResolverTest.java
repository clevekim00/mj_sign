package com.mj.sign;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SignLanguageResolverTest {

    @Test
    void resolvesEnglishLocaleToAslAndSignGemma() {
        SignLanguageResolver resolver = new SignLanguageResolver(new SignLanguageProperties());

        InferenceContext context = resolver.resolve("en-US", null, null);

        assertEquals("en-US", context.locale());
        assertEquals("asl", context.sign_language());
        assertEquals("sign-gemma", context.model_profile());
        assertEquals("signbridge-model-v1", context.protocol_version());
    }

    @Test
    void usesExplicitRegisteredSignLanguageAndModelProfileWhenProvided() {
        SignLanguageResolver resolver = new SignLanguageResolver(new SignLanguageProperties());

        InferenceContext context = resolver.resolve("ko-KR", "asl", "sign-gemma", "mj-sign-model-v2");

        assertEquals("ko-KR", context.locale());
        assertEquals("asl", context.sign_language());
        assertEquals("sign-gemma", context.model_profile());
        assertEquals("mj-sign-model-v2", context.protocol_version());
    }

    @Test
    void rejectsUnsupportedExplicitSignLanguage() {
        SignLanguageResolver resolver = new SignLanguageResolver(new SignLanguageProperties());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve("en-US", "xyz", null)
        );

        assertEquals("unsupported sign_language: xyz", error.getMessage());
    }

    @Test
    void rejectsUnsupportedExplicitModelProfile() {
        SignLanguageResolver resolver = new SignLanguageResolver(new SignLanguageProperties());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve("en-US", "asl", "custom-english-model")
        );

        assertEquals("unsupported model_profile: custom-english-model", error.getMessage());
    }

    @Test
    void rejectsModelProfileThatBelongsToAnotherSignLanguage() {
        SignLanguageResolver resolver = new SignLanguageResolver(new SignLanguageProperties());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve("en-US", "asl", "sign-gemma-ko")
        );

        assertEquals("model_profile sign-gemma-ko is not registered for sign_language asl", error.getMessage());
    }

    @Test
    void canonicalizesLegacyProtocolVersion() {
        SignLanguageResolver resolver = new SignLanguageResolver(new SignLanguageProperties());

        InferenceContext context = resolver.resolve("en-US", null, null, "mj-sign-model-v1");

        assertEquals("signbridge-model-v1", context.protocol_version());
    }
}
