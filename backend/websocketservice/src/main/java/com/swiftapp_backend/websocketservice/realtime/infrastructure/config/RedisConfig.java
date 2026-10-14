package com.swiftapp_backend.websocketservice.realtime.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.websocketservice.realtime.infrastructure.realtime.redis.RedisSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public GenericJackson2JsonRedisSerializer redisJsonSerializer(
            ObjectMapper objectMapper
    ) {
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory factory,
            MessageListenerAdapter adapter,
            @Value("${app.redis.listener.auto-startup:true}") boolean autoStartup
    ) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer() {
            @Override
            public boolean isAutoStartup() {
                return autoStartup;
            }
        };
        container.setConnectionFactory(factory);
        container.addMessageListener(adapter, new PatternTopic("ride-updates"));
        container.addMessageListener(adapter, new PatternTopic("driver-updates"));
        container.addMessageListener(adapter, new PatternTopic("chat-messages"));
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(
            RedisSubscriber subscriber,
            GenericJackson2JsonRedisSerializer redisJsonSerializer
    ) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "receiveMessage");
        adapter.setSerializer(redisJsonSerializer);
        return adapter;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory factory,
            GenericJackson2JsonRedisSerializer redisJsonSerializer
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(redisJsonSerializer);
        template.setHashValueSerializer(redisJsonSerializer);
        template.afterPropertiesSet();

        return template;
    }
}
