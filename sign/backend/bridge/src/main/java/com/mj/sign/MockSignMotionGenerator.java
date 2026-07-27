package com.mj.sign;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MockSignMotionGenerator implements SignMotionGenerator {
    private static final int DEFAULT_FPS = 12;

    @Override
    public SignSynthesisMotion generate(SignSynthesisPlan plan, SignSynthesisContext context) {
        int frameCount = Math.max(18, Math.min(48, plan.glosses().size() * 8));
        List<SignSynthesisFrame> frames = new ArrayList<>(frameCount);
        for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
            long timestampMs = Math.round(frameIndex * (1000.0 / DEFAULT_FPS));
            double progress = frameIndex / (double) Math.max(1, frameCount - 1);
            frames.add(new SignSynthesisFrame(
                    timestampMs,
                    buildPoints(21, progress, 0.36, 0.48, 0.0),
                    buildPoints(21, progress, 0.64, 0.48, 1.1),
                    buildPoints(8, progress, 0.50, 0.62, 2.2),
                    buildPoints(12, progress, 0.50, 0.24, 3.3)
            ));
        }
        return new SignSynthesisMotion("landmark-frames", DEFAULT_FPS, frameCount, List.copyOf(frames));
    }

    private List<SignSynthesisPoint> buildPoints(
            int count,
            double progress,
            double centerX,
            double centerY,
            double phaseOffset
    ) {
        List<SignSynthesisPoint> points = new ArrayList<>(count);
        for (int pointIndex = 0; pointIndex < count; pointIndex++) {
            double phase = (progress * Math.PI * 2.0) + phaseOffset + (pointIndex * 0.19);
            double spread = 0.07 + ((pointIndex % 5) * 0.012);
            double x = clamp(centerX + Math.sin(phase) * spread + ((pointIndex % 4) - 1.5) * 0.012);
            double y = clamp(centerY + Math.cos(phase * 0.8) * spread + ((pointIndex / 4) * 0.01));
            double z = Math.cos(phase) * 0.06;
            points.add(new SignSynthesisPoint(round(x), round(y), round(z)));
        }
        return List.copyOf(points);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
