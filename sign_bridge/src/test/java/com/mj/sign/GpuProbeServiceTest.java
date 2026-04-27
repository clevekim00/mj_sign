package com.mj.sign;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GpuProbeServiceTest {

    @Test
    void queueProviderWithMissingKafkaAdapterIsNotReady() {
        GpuServingProperties properties = properties();
        properties.setProvider("queue");
        properties.setQueueTransport("kafka");
        properties.setQueueBrokerMode("spring");

        GpuProbeService service = new GpuProbeService(
                new RestTemplateBuilder(),
                properties,
                emptyProvider(),
                emptyProvider()
        ) {
            @Override
            protected ProbeStatus probeHttpBackend() {
                return new ProbeStatus(true, Map.of("probe_url", "http://gpu/health"));
            }
        };

        GpuProbeService.ProbeStatus result = service.probe();

        assertFalse(result.healthy());
        assertEquals("kafka", result.details().get("queue_transport"));
        assertEquals(false, result.details().get("broker_adapter_present"));
    }

    @Test
    void queueProviderWithAdapterAndHealthyBackendIsReady() {
        GpuServingProperties properties = properties();
        properties.setProvider("queue");
        properties.setQueueTransport("rabbitmq");
        properties.setQueueBrokerMode("spring");

        GpuProbeService service = new GpuProbeService(
                new RestTemplateBuilder(),
                properties,
                emptyProvider(),
                fixedProvider(new RabbitMqBrokerPort() {
                    @Override
                    public void publish(QueueBrokerMessage message) {
                    }
                })
        ) {
            @Override
            protected ProbeStatus probeHttpBackend() {
                return new ProbeStatus(true, Map.of("probe_url", "http://gpu/health"));
            }
        };

        GpuProbeService.ProbeStatus result = service.probe();

        assertTrue(result.healthy());
        assertEquals("rabbitmq", result.details().get("queue_transport"));
        assertEquals(true, result.details().get("broker_adapter_present"));
    }

    @Test
    void grpcProviderIsNotReadyUntilImplemented() {
        GpuServingProperties properties = properties();
        properties.setProvider("grpc");

        GpuProbeService service = new GpuProbeService(
                new RestTemplateBuilder(),
                properties,
                emptyProvider(),
                emptyProvider()
        );

        GpuProbeService.ProbeStatus result = service.probe();

        assertFalse(result.healthy());
        assertEquals("grpc", result.details().get("provider"));
    }

    private static GpuServingProperties properties() {
        GpuServingProperties properties = new GpuServingProperties();
        properties.setProvider("http");
        properties.setBaseUrl("http://localhost:8000");
        properties.setInferPath("/api/v2/recognize");
        properties.setHealthPath("/");
        return properties;
    }

    private static <T> ObjectProvider<T> emptyProvider() {
        return new SimpleObjectProvider<>(null);
    }

    private static <T> ObjectProvider<T> fixedProvider(T instance) {
        return new SimpleObjectProvider<>(instance);
    }

    private static final class SimpleObjectProvider<T> implements ObjectProvider<T> {
        private final T instance;

        private SimpleObjectProvider(T instance) {
            this.instance = instance;
        }

        @Override
        public T getObject(Object... args) {
            return instance;
        }

        @Override
        public T getIfAvailable() {
            return instance;
        }

        @Override
        public T getIfUnique() {
            return instance;
        }

        @Override
        public T getObject() {
            return instance;
        }

        @Override
        public Iterator<T> iterator() {
            return instance == null
                    ? Collections.emptyIterator()
                    : Collections.singleton(instance).iterator();
        }
    }
}
