package com.mj.sign;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.Executor;

@Service
@ConditionalOnProperty(prefix = "sign.gpu", name = "queue-broker-mode", havingValue = "loopback", matchIfMissing = true)
public class KafkaWorkerLoop implements KafkaBrokerPort {

    private final QueueWorkerBackend workerBackend;
    private final QueueReplyStore replyStore;
    private final Executor inferenceExecutor;

    public KafkaWorkerLoop(
            QueueWorkerBackend workerBackend,
            QueueReplyStore replyStore,
            @Qualifier("inferenceExecutor") Executor inferenceExecutor
    ) {
        this.workerBackend = workerBackend;
        this.replyStore = replyStore;
        this.inferenceExecutor = inferenceExecutor;
    }

    @Override
    public void publish(QueueBrokerMessage message) {
        inferenceExecutor.execute(() -> {
            QueueInferenceTask task = new QueueInferenceTask(
                    message.requestId(),
                    message.sessionId(),
                    message.requestTopic(),
                    message.payload(),
                    message.createdAt()
            );
            QueueInferenceResult result = workerBackend.process(task);
            replyStore.complete(new QueueInferenceResult(
                    message.requestId(),
                    message.sessionId(),
                    result.response(),
                    Instant.now()
            ));
        });
    }
}
