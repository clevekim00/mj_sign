package com.mj.sign;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v2")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
public class SignSynthesisController {
    private final SignSynthesisService signSynthesisService;

    public SignSynthesisController(SignSynthesisService signSynthesisService) {
        this.signSynthesisService = signSynthesisService;
    }

    @Operation(
            summary = "Synthesize sign motion from text",
            description = "Creates a deterministic sign plan and motion payload for the selected profile route.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Text-to-sign request. Omit profile fields to use bridge defaults.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "text-to-sign",
                                    value = """
                                            {
                                              "session_id": "demo-t2s-001",
                                              "text": "안녕하세요",
                                              "locale": "ko-KR",
                                              "sign_language": "ksl",
                                              "model_profile": "sign-gemma-ko",
                                              "output_format": "landmark_motion",
                                              "protocol_version": "v1"
                                            }
                                            """
                            )
                    )
            ),
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Generated sign plan and playback motion.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "synthesis-result",
                                    value = """
                                            {
                                              "session_id": "demo-t2s-001",
                                              "event_type": "sign_synthesis.result",
                                              "source_type": "text",
                                              "text": "안녕하세요",
                                              "locale": "ko-KR",
                                              "sign_language": "ksl",
                                              "model_profile": "sign-gemma-ko",
                                              "protocol_version": "v1",
                                              "sign_plan": {
                                                "glosses": ["안녕하세요"],
                                                "non_manual_markers": ["neutral"],
                                                "grammar_note": "mock planner"
                                              },
                                              "motion": {
                                                "format": "landmark_motion",
                                                "fps": 30,
                                                "frame_count": 2,
                                                "frames": [
                                                  {
                                                    "timestamp_ms": 0,
                                                    "left_hand": [{"x": 0.1, "y": 0.2, "z": 0.0}],
                                                    "right_hand": [{"x": 0.7, "y": 0.2, "z": 0.0}],
                                                    "pose": [],
                                                    "face_contour": []
                                                  }
                                                ]
                                              },
                                              "is_final": true,
                                              "confidence": 0.91,
                                              "error": null
                                            }
                                            """
                            )
                    )
            )
    )
    @PostMapping("/sign/synthesize")
    public SignSynthesisResult synthesizeText(@RequestBody SignSynthesisRequest request) {
        return synthesize(withSourceType(request, "text"));
    }

    @Operation(
            summary = "Synthesize sign motion from speech input",
            description = "Accepts either a transcript or base64 audio field and returns the same sign playback envelope as text synthesis.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Speech-to-sign request. Current mock path primarily uses transcript text.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "speech-to-sign",
                                    value = """
                                            {
                                              "session_id": "demo-sts-001",
                                              "transcript": "good morning",
                                              "locale": "en-US",
                                              "sign_language": "asl",
                                              "model_profile": "sign-gemma",
                                              "output_format": "landmark_motion",
                                              "protocol_version": "v1"
                                            }
                                            """
                            )
                    )
            ),
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Generated sign plan and playback motion.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "speech-synthesis-result",
                                    value = """
                                            {
                                              "session_id": "demo-sts-001",
                                              "event_type": "sign_synthesis.result",
                                              "source_type": "speech",
                                              "text": "good morning",
                                              "locale": "en-US",
                                              "sign_language": "asl",
                                              "model_profile": "sign-gemma",
                                              "protocol_version": "v1",
                                              "sign_plan": {
                                                "glosses": ["GOOD", "MORNING"],
                                                "non_manual_markers": ["neutral"],
                                                "grammar_note": "mock planner"
                                              },
                                              "motion": {
                                                "format": "landmark_motion",
                                                "fps": 30,
                                                "frame_count": 2,
                                                "frames": []
                                              },
                                              "is_final": true,
                                              "confidence": 0.9,
                                              "error": null
                                            }
                                            """
                            )
                    )
            )
    )
    @PostMapping("/speech/sign")
    public SignSynthesisResult synthesizeSpeech(@RequestBody SignSynthesisRequest request) {
        return synthesize(withSourceType(request, "speech"));
    }

    private SignSynthesisResult synthesize(SignSynthesisRequest request) {
        try {
            return signSynthesisService.synthesize(request);
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage(), error);
        } catch (IllegalStateException error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, error.getMessage(), error);
        }
    }

    private SignSynthesisRequest withSourceType(SignSynthesisRequest request, String sourceType) {
        if (request == null) {
            return new SignSynthesisRequest(null, sourceType, null, null, null, null, null, null, null, null);
        }
        return request.withSourceType(sourceType);
    }
}
