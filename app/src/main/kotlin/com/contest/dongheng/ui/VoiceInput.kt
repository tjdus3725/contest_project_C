package com.contest.dongheng.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 음성 입력. Android SpeechRecognizer(한국어) 사용.
 * Pleos SpeechToText SDK로 교체할 때는 [start]/[stop] 두 곳만 바꾸면 된다.
 */
class VoiceInput(private val ctx: Context) {
    var listening by mutableStateOf(false)
        private set
    /** 인식 중간 결과 또는 오류 안내 */
    var message by mutableStateOf("")
        private set

    private var recognizer: SpeechRecognizer? = null

    val available get() = SpeechRecognizer.isRecognitionAvailable(ctx)

    fun start(onText: (String) -> Unit) {
        if (listening) return
        if (!available) {
            message = "이 기기에서는 음성 인식을 사용할 수 없어요. 문장으로 입력해 주세요."
            return
        }
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(ctx).also { recognizer = it }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { message = "듣고 있어요... 말씀해 주세요" }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    ?.let { message = it }
            }

            override fun onResults(results: Bundle?) {
                listening = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (text.isNullOrBlank()) {
                    message = "잘 듣지 못했어요. 다시 말씀해 주세요."
                } else {
                    message = ""
                    onText(text)
                }
            }

            override fun onError(error: Int) {
                listening = false
                message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        "잘 듣지 못했어요. 다시 말씀해 주세요."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "마이크 권한이 필요해요."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        "음성 인식에 통신이 필요해요. 문장으로 입력해 주세요."
                    else -> "음성 인식에 실패했어요. 문장으로 입력해 주세요. (오류 $error)"
                }
            }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        listening = true
        message = "듣고 있어요... 말씀해 주세요"
        runCatching { r.startListening(intent) }.onFailure {
            listening = false
            message = "음성 인식을 시작하지 못했어요."
        }
    }

    fun stop() {
        runCatching { recognizer?.stopListening() }
        listening = false
    }

    fun release() {
        runCatching { recognizer?.destroy() }
        recognizer = null
        listening = false
    }
}
