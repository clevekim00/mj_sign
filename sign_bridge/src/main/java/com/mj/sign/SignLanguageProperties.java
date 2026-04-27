package com.mj.sign;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "sign.language")
public class SignLanguageProperties {

    private String defaultLocale = "ko-KR";
    private String defaultSignLanguage = "ksl";
    private String defaultModelProfile = "sign-gemma-ko";
    private String protocolVersion = InferenceContext.DEFAULT_PROTOCOL_VERSION;
    private Map<String, String> signLanguageByLocaleLanguage = new HashMap<>(Map.of(
            "ko", "ksl",
            "en", "asl",
            "ja", "jsl",
            "zh", "csl",
            "fr", "lsf",
            "de", "dgs",
            "es", "lse"
    ));
    private Map<String, String> modelProfileBySignLanguage = new HashMap<>(Map.of(
            "ksl", "sign-gemma-ko",
            "asl", "sign-gemma",
            "jsl", "sign-gemma-ja",
            "csl", "sign-gemma-zh",
            "lsf", "sign-gemma-fr",
            "dgs", "sign-gemma-de",
            "lse", "sign-gemma-es"
    ));

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public String getDefaultSignLanguage() {
        return defaultSignLanguage;
    }

    public void setDefaultSignLanguage(String defaultSignLanguage) {
        this.defaultSignLanguage = defaultSignLanguage;
    }

    public String getDefaultModelProfile() {
        return defaultModelProfile;
    }

    public void setDefaultModelProfile(String defaultModelProfile) {
        this.defaultModelProfile = defaultModelProfile;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public Map<String, String> getSignLanguageByLocaleLanguage() {
        return signLanguageByLocaleLanguage;
    }

    public void setSignLanguageByLocaleLanguage(Map<String, String> signLanguageByLocaleLanguage) {
        this.signLanguageByLocaleLanguage = signLanguageByLocaleLanguage;
    }

    public Map<String, String> getModelProfileBySignLanguage() {
        return modelProfileBySignLanguage;
    }

    public void setModelProfileBySignLanguage(Map<String, String> modelProfileBySignLanguage) {
        this.modelProfileBySignLanguage = modelProfileBySignLanguage;
    }
}
