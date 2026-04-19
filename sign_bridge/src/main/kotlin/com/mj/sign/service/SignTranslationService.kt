package com.mj.sign.service

import org.springframework.stereotype.Service

/**
 * 수화 키워드를 자연스러운 한국어 문장으로 정제하는 비즈니스 로직 담당 서비스
 */
@Service
class SignTranslationService(
    private val aiProvider: AiTranslationProvider
) {

    private val systemPrompt = """
        당신은 전문 수어 통역사입니다. 
        입력되는 데이터는 수어 화자가 표현한 단어들의 나열입니다. 
        이 단어들을 한국어 문법에 맞게 윤문하여 아주 자연스럽고 공손한 한국어 문장으로 변환해 주세요.
        
        [지시 사항]
        1. 불필요한 사족이나 설명을 달지 말고, 변환된 문장만 출력하세요.
        2. '나', '나의' 등의 표현은 상황에 따라 '저', '제가' 등 공손한 표현으로 바꾸어 주세요.
        3. 문장의 끝을 자연스럽게(예: ~해요, ~입니다) 맺어 주세요.
    """.trimIndent()

    /**
     * 키워드 목록을 받아 자연스러운 문장으로 변환
     */
    fun translateKeywords(keywords: List<String>): String {
        if (keywords.isEmpty()) return "입력된 키워드가 없습니다."
        
        val userPrompt = "수어 키워드: ${keywords.joinToString(", ")}"
        return aiProvider.generateResponse(systemPrompt, userPrompt)
    }
}
