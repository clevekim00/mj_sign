package com.mj.sign;

import org.springframework.stereotype.Service;

@Service("mockSignSynthesisProvider")
public class MockSignSynthesisProvider implements SignSynthesisProvider {
    private final SignPlanner signPlanner;
    private final SignMotionGenerator motionGenerator;

    public MockSignSynthesisProvider(SignPlanner signPlanner, SignMotionGenerator motionGenerator) {
        this.signPlanner = signPlanner;
        this.motionGenerator = motionGenerator;
    }

    @Override
    public SignSynthesisResult synthesize(SignSynthesisRequest request, SignSynthesisContext context) {
        SignSynthesisPlan plan = signPlanner.plan(context);
        SignSynthesisMotion motion = motionGenerator.generate(plan, context);
        return new SignSynthesisResult(
                context.session_id(),
                "synthesis_result",
                context.source_type(),
                context.text(),
                context.locale(),
                context.sign_language(),
                context.model_profile(),
                context.protocol_version(),
                plan,
                motion,
                true,
                "speech".equals(context.source_type()) ? 0.76 : 0.82,
                null
        );
    }
}
