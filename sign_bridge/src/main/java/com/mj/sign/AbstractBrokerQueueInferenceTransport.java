package com.mj.sign;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

abstract class AbstractBrokerQueueInferenceTransport implements QueueInferenceTransport {

    protected final GpuServingProperties properties;
    protected final QueueReplyStore replyStore;

    protected AbstractBrokerQueueInferenceTransport(
            GpuServingProperties properties,
            QueueReplyStore replyStore
    ) {
        this.properties = properties;
        this.replyStore = replyStore;
    }

    @Override
    public QueueInferenceResult submitAndAwait(QueueInferenceTask task, Duration timeout) {
        QueueBrokerMessage message = toBrokerMessage(task);
        replyStore.registerRequest(task.requestId());
        try {
            publish(message);
            QueueInferenceResult result = replyStore.await(task.requestId(), timeout);
            if ("unknown-session".equals(result.sessionId())) {
                return timeoutResult(task, timeout, message);
            }
            return result;
        } catch (Exception error) {
            return failureResult(task, "Queue transport failed: " + error.getMessage());
        }
    }

    protected QueueBrokerMessage toBrokerMessage(QueueInferenceTask task) {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("request_id", task.requestId());
        headers.put("session_id", task.sessionId());
        headers.put("transport", "queue");
        headers.put("provider", properties.getProvider());
        headers.put("queue_transport", properties.getQueueTransport());

        return new QueueBrokerMessage(
                task.requestId(),
                task.sessionId(),
                properties.getQueueRequestTopic(),
                properties.getQueueResultTopic(),
                properties.getQueueConsumerGroup(),
                properties.getQueueRoutingKey(),
                headers,
                task.request(),
                task.submittedAt()
        );
    }

    protected abstract void publish(QueueBrokerMessage message);

    protected QueueInferenceResult timeoutResult(
            QueueInferenceTask task,
            Duration timeout,
            QueueBrokerMessage message
    ) {
        String error = "Queue transport '" + kind().configValue()
                + "' timed out. requestTopic="
                + message.requestTopic()
                + ", resultTopic="
                + message.resultTopic()
                + ", timeoutMs="
                + timeout.toMillis();
        return failureResult(task, error);
    }

    protected QueueInferenceResult failureResult(QueueInferenceTask task, String error) {
        return new QueueInferenceResult(
                task.requestId(),
                task.sessionId(),
                new GpuInferenceResponse(
                        task.sessionId(),
                        null,
                        true,
                        0,
                        0,
                        null,
                        error
                ),
                Instant.now()
        );
    }
}
