package com.saju.api

import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ErrorResponse(val message: String)

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(e: IllegalArgumentException): ErrorResponse =
        ErrorResponse(e.message ?: "잘못된 입력입니다")

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleUnreadable(e: HttpMessageNotReadableException): ErrorResponse =
        ErrorResponse("요청 본문을 해석할 수 없습니다: 필수 필드(year, month, day, hour, gender)를 확인하세요")

    @ExceptionHandler(com.saju.reading.ReadingUnavailableException::class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun handleReadingUnavailable(e: com.saju.reading.ReadingUnavailableException): ErrorResponse =
        ErrorResponse(e.message ?: "LLM 해석 서비스를 사용할 수 없습니다")

    @ExceptionHandler(com.saju.reading.ReadingGenerationException::class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    fun handleReadingGeneration(e: com.saju.reading.ReadingGenerationException): ErrorResponse =
        ErrorResponse(e.message ?: "해석문 생성에 실패했습니다")
}
