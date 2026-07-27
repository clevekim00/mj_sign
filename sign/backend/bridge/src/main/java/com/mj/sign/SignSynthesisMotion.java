package com.mj.sign;

import java.util.List;

public record SignSynthesisMotion(
        String format,
        int fps,
        int frame_count,
        List<SignSynthesisFrame> frames
) {
}
