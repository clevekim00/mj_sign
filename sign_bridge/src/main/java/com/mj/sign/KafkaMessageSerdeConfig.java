package com.mj.sign;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "sign.gpu", name = "queue-broker-mode", havingValue = "spring")
public class KafkaMessageSerdeConfig {

    @Bean
    public ProducerFactory<String, QueueBrokerMessage> queueBrokerMessageProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, QueueBrokerMessage> queueBrokerMessageKafkaTemplate(
            ProducerFactory<String, QueueBrokerMessage> queueBrokerMessageProducerFactory
    ) {
        return new KafkaTemplate<>(queueBrokerMessageProducerFactory);
    }

    @Bean
    public ProducerFactory<String, QueueBrokerReplyMessage> queueBrokerReplyProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, QueueBrokerReplyMessage> queueBrokerReplyKafkaTemplate(
            ProducerFactory<String, QueueBrokerReplyMessage> queueBrokerReplyProducerFactory
    ) {
        return new KafkaTemplate<>(queueBrokerReplyProducerFactory);
    }

    @Bean
    public ConsumerFactory<String, QueueBrokerMessage> queueBrokerRequestConsumerFactory(
            KafkaProperties kafkaProperties
    ) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.mj.sign");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, QueueBrokerMessage.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(QueueBrokerMessage.class, false)
        );
    }

    @Bean
    public ConsumerFactory<String, QueueBrokerReplyMessage> queueBrokerReplyConsumerFactory(
            KafkaProperties kafkaProperties
    ) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.mj.sign");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, QueueBrokerReplyMessage.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(QueueBrokerReplyMessage.class, false)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, QueueBrokerReplyMessage> queueBrokerReplyKafkaListenerContainerFactory(
            ConsumerFactory<String, QueueBrokerReplyMessage> queueBrokerReplyConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, QueueBrokerReplyMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(queueBrokerReplyConsumerFactory);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, QueueBrokerMessage> queueBrokerRequestKafkaListenerContainerFactory(
            ConsumerFactory<String, QueueBrokerMessage> queueBrokerRequestConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, QueueBrokerMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(queueBrokerRequestConsumerFactory);
        return factory;
    }
}
