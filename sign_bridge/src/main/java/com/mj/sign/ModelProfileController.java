package com.mj.sign;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v2")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
public class ModelProfileController {

    private static final Map<String, String> DEFAULT_LOCALE_BY_LANGUAGE = Map.of(
            "ko", "ko-KR",
            "en", "en-US",
            "ja", "ja-JP",
            "zh", "zh-CN",
            "fr", "fr-FR",
            "de", "de-DE",
            "es", "es-ES"
    );

    private final SignLanguageProperties properties;
    private final SignLanguageResolver resolver;

    public ModelProfileController(SignLanguageProperties properties, SignLanguageResolver resolver) {
        this.properties = properties;
        this.resolver = resolver;
    }

    @Operation(
            summary = "Discover supported model profiles",
            description = "Returns the locale, sign language, model profile, and protocol version routes supported by this bridge.",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Profile catalog used by clients before WebSocket, T2S, or STS requests.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "model-profiles",
                                    value = """
                                            {
                                              "default_profile": {
                                                "locale": "ko-KR",
                                                "sign_language": "ksl",
                                                "model_profile": "sign-gemma-ko",
                                                "protocol_version": "v1",
                                                "is_default": true
                                              },
                                              "profiles": [
                                                {
                                                  "locale": "ko-KR",
                                                  "sign_language": "ksl",
                                                  "model_profile": "sign-gemma-ko",
                                                  "protocol_version": "v1",
                                                  "is_default": true
                                                },
                                                {
                                                  "locale": "en-US",
                                                  "sign_language": "asl",
                                                  "model_profile": "sign-gemma",
                                                  "protocol_version": "v1",
                                                  "is_default": false
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
    )
    @GetMapping("/model-profiles")
    public ModelProfileCatalog listProfiles() {
        InferenceContext defaultContext = resolver.defaults();
        List<ModelProfileInfo> profiles = new ArrayList<>();

        properties.getSignLanguageByLocaleLanguage().forEach((language, signLanguage) -> {
            String locale = localeTagFor(language);
            String modelProfile = properties.getModelProfileBySignLanguage()
                    .getOrDefault(signLanguage, properties.getDefaultModelProfile());
            InferenceContext context = resolver.resolve(locale, signLanguage, modelProfile);
            profiles.add(toProfileInfo(context, defaultContext));
        });

        ModelProfileInfo defaultProfile = toProfileInfo(defaultContext, defaultContext);
        if (profiles.stream().noneMatch(profile -> profile.matches(defaultProfile))) {
            profiles.add(defaultProfile);
        }

        profiles.sort(Comparator
                .comparing(ModelProfileInfo::is_default).reversed()
                .thenComparing(ModelProfileInfo::locale)
                .thenComparing(ModelProfileInfo::sign_language));

        return new ModelProfileCatalog(defaultProfile, List.copyOf(profiles));
    }

    private ModelProfileInfo toProfileInfo(InferenceContext context, InferenceContext defaultContext) {
        return new ModelProfileInfo(
                context.locale(),
                context.sign_language(),
                context.model_profile(),
                context.protocol_version(),
                context.locale().equals(defaultContext.locale())
                        && context.sign_language().equals(defaultContext.sign_language())
                        && context.model_profile().equals(defaultContext.model_profile())
        );
    }

    private String localeTagFor(String language) {
        String normalized = language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
        return DEFAULT_LOCALE_BY_LANGUAGE.getOrDefault(normalized, normalized);
    }

    public record ModelProfileCatalog(
            ModelProfileInfo default_profile,
            List<ModelProfileInfo> profiles
    ) {
    }

    public record ModelProfileInfo(
            String locale,
            String sign_language,
            String model_profile,
            String protocol_version,
            boolean is_default
    ) {
        boolean matches(ModelProfileInfo other) {
            return locale.equals(other.locale)
                    && sign_language.equals(other.sign_language)
                    && model_profile.equals(other.model_profile)
                    && protocol_version.equals(other.protocol_version);
        }
    }
}
