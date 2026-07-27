package com.mj.sign;

import java.util.List;

public record SignSynthesisFrame(
        long timestamp_ms,
        List<SignSynthesisPoint> left_hand,
        List<SignSynthesisPoint> right_hand,
        List<SignSynthesisPoint> pose,
        List<SignSynthesisPoint> face_contour
) {
}
