package com.ticketmesh.ai.config;

import com.ticketmesh.ai.model.OfflineChatModel;
import com.ticketmesh.ai.model.OfflineEmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Self-contained AI provider used when no external LLM key is configured. The
 * beans are deterministic and offline so the application starts and all tests
 * run without network access. Switching to a hosted provider is a profile
 * change that supplies Spring AI's {@code ChatModel}/{@code EmbeddingModel}
 * implementations instead.
 *
 * <p>The beans are registered as {@code @ConditionalOnMissingBean} so they
 * always serve as a deterministic fallback whenever a hosted provider is not
 * wired. This makes the platform fully runnable in offline / CI environments.
 */
@Configuration
public class AiOfflineConfig {

    private static final Logger log = LoggerFactory.getLogger(AiOfflineConfig.class);

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel chatModel() {
        log.info("Registering offline ChatModel (deterministic, no LLM provider)");
        return new OfflineChatModel();
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel embeddingModel() {
        log.info("Registering offline EmbeddingModel (deterministic n-gram hashing)");
        return new OfflineEmbeddingModel();
    }
}
