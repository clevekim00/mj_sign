package com.mj.sign.controller

import com.mj.sign.service.SignTranslationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 수업 인식 클라이언트의 번역 요청을 처리하는 컨트롤러
 */
@Tag(name = "Sign Translation", description = "수어 번역 및 문장 보정 API")
@RestController
@RequestMapping("/api/v2")
class SignController(
    private val translationService: SignTranslationService
) {

    @Operation(
        summary = "수어 키워드 번역",
        description = "인식된 수어 키워드 목록을 받아 Gemma 2 LLM을 통해 자연스러운 한국어 문장으로 변환합니다."
    )
    @PostMapping("/translate")
    fun translate(@RequestBody request: TranslationRequest): TranslationResponse {
        val result = translationService.translateKeywords(request.keywords)
        return TranslationResponse(translatedText = result)
    }
}

/**
 * 요청 DTO
 */
@Schema(description = "번역 요청 데이터")
data class TranslationRequest(
    @Schema(description = "인식된 수어 키워드 목록", example = "[\"나\", \"밥\", \"먹다\"]")
    val keywords: List<String> = emptyList()
)

@Schema(description = "번역 응답 데이터")
data class TranslationResponse(
    @Schema(description = "LLM으로 보정된 자연스러운 한국어 문장", example = "나는 밥을 먹었습니다.")
    val translatedText: String
)
