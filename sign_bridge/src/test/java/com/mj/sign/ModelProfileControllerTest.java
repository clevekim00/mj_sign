package com.mj.sign;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelProfileControllerTest {

    @Test
    void listsDefaultAndSupportedModelProfiles() {
        SignLanguageProperties properties = new SignLanguageProperties();
        ModelProfileController controller = new ModelProfileController(
                properties,
                new SignLanguageResolver(properties)
        );

        ModelProfileController.ModelProfileCatalog catalog = controller.listProfiles();

        assertEquals("ko-KR", catalog.default_profile().locale());
        assertEquals("ksl", catalog.default_profile().sign_language());
        assertEquals("sign-gemma-ko", catalog.default_profile().model_profile());
        assertEquals("signbridge-model-v1", catalog.default_profile().protocol_version());
        assertTrue(catalog.default_profile().is_default());
        assertTrue(catalog.profiles().stream().anyMatch(profile ->
                profile.locale().equals("en-US")
                        && profile.sign_language().equals("asl")
                        && profile.model_profile().equals("sign-gemma")
        ));
    }
}
