package com.mj.sign;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v2")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class SignSynthesisController {
    private final SignSynthesisService signSynthesisService;

    public SignSynthesisController(SignSynthesisService signSynthesisService) {
        this.signSynthesisService = signSynthesisService;
    }

    @PostMapping("/sign/synthesize")
    public SignSynthesisResult synthesizeText(@RequestBody SignSynthesisRequest request) {
        return synthesize(withSourceType(request, "text"));
    }

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
