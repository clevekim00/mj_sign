package com.mj.sign.service

import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

/**
 * AI 모델(Gemma 2)과의 통신을 담당하는 서비스 인터페이스
 */
interface AiTranslationProvider {
    fun generateResponse(systemPrompt: String, userPrompt: String): String
}

/**
 * Spring AI ChatClient를 사용하여 Ollama 서버의 Gemma 2 모델과 통신하는 구현체
 */
@Service
class GemmaService(
    chatClientBuilder: ChatClient.Builder
) : AiTranslationProvider {

    private val chatClient: ChatClient = chatClientBuilder.build()

    override fun generateResponse(systemPrompt: String, userPrompt: String): String {
        return chatClient.prompt()
            .system(systemPrompt)
            .user(userPrompt)
            .call()
            .content() ?: "번역된 문장을 생성할 수 없습니다."
    }
}
