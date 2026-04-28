package com.mj.sign;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignSynthesisServiceTest {
    private final SignSynthesisService service = new SignSynthesisService();

    @Test
    void synthesizesKoreanTextToKslMotion() {
        SignSynthesisResult result = service.synthesize(new SignSynthesisRequest(
                "t2s-ko",
                "text",
                "내일 병원에 가야 합니다.",
                null,
                null,
                "ko-KR",
                "ksl",
                "sign-gemma-ko",
                "landmarks",
                null
        ));

        assertEquals("t2s-ko", result.session_id());
        assertEquals("text", result.source_type());
        assertEquals("ksl", result.sign_language());
        assertEquals(SignSynthesisService.PROTOCOL_VERSION, result.protocol_version());
        assertTrue(result.sign_plan().glosses().contains("병원"));
        assertTrue(result.motion().frame_count() > 0);
        assertEquals(result.motion().frame_count(), result.motion().frames().size());
        assertEquals(21, result.motion().frames().getFirst().left_hand().size());
    }

    @Test
    void synthesizesEnglishTranscriptToAslDefaults() {
        SignSynthesisResult result = service.synthesize(new SignSynthesisRequest(
                "sts-en",
                "speech",
                null,
                "I need help tomorrow!",
                null,
                "en-US",
                null,
                null,
                null,
                null
        ));

        assertEquals("speech", result.source_type());
        assertEquals("asl", result.sign_language());
        assertEquals("sign-gemma", result.model_profile());
        assertTrue(result.sign_plan().glosses().contains("NEED"));
        assertTrue(result.sign_plan().non_manual_markers().contains("emphasis"));
        assertFalse(result.motion().frames().isEmpty());
    }

    @Test
    void rejectsMissingTextAndTranscript() {
        SignSynthesisRequest request = new SignSynthesisRequest(
                "missing-input",
                "text",
                null,
                null,
                null,
                "ko-KR",
                "ksl",
                null,
                null,
                null
        );

        assertThrows(IllegalArgumentException.class, () -> service.synthesize(request));
    }
}
