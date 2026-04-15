package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;

public record BufferedChunkResult(
        boolean readyForInference,
        ClientStreamChunk chunk,
        int bufferedFrameCount,
        long scheduleToken,
        boolean idleTimeoutTriggered
) {
}
