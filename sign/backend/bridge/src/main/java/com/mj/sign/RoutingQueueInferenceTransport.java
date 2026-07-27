package com.mj.sign;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Primary
@Service
public class RoutingQueueInferenceTransport implements QueueInferenceTransport {

    private final GpuServingProperties properties;
    private final Map<QueueTransportKind, QueueInferenceTransport> transports;

    public RoutingQueueInferenceTransport(
            GpuServingProperties properties,
            List<QueueInferenceTransport> transportImplementations
    ) {
        this.properties = properties;
        this.transports = new EnumMap<>(QueueTransportKind.class);
        for (QueueInferenceTransport transport : transportImplementations) {
            if (transport instanceof RoutingQueueInferenceTransport) {
                continue;
            }
            transports.put(transport.kind(), transport);
        }
    }

    @Override
    public QueueTransportKind kind() {
        return selectedKind();
    }

    @Override
    public QueueInferenceResult submitAndAwait(QueueInferenceTask task, Duration timeout) {
        QueueInferenceTransport delegate = transports.get(selectedKind());
        if (delegate == null) {
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
                            "No queue transport registered for kind: " + properties.getQueueTransport()
                    ),
                    task.submittedAt()
            );
        }
        return delegate.submitAndAwait(task, timeout);
    }

    QueueTransportKind selectedKind() {
        return QueueTransportKind.fromConfig(properties.getQueueTransport());
    }
}
