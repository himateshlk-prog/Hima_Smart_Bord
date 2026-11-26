package com.hima.smartboard

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.TextView

class SinhalaKeyboardService : InputMethodService() {

    private val buffer = StringBuilder()
    private var previewText: TextView? = null

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        previewText = view.findViewById(R.id.tvPreview)

        view.findViewById<Button>(R.id.btnK).setOnClickListener { appendChar("k") }
        view.findViewById<Button>(R.id.btnA).setOnClickListener { appendChar("a") }
        view.findViewById<Button>(R.id.btnM).setOnClickListener { appendChar("m") }

        view.findViewById<Button>(R.id.btnDel).setOnClickListener {
            if (buffer.isNotEmpty()) {
                buffer.deleteCharAt(buffer.length - 1)
                updateComposing()
            } else {
                currentInputConnection?.deleteSurroundingText(1, 0)
            }
        }

        view.findViewById<Button>(R.id.btnSpace).setOnClickListener {
            commitSinhala()
            currentInputConnection?.commitText(" ", 1)
        }

        view.findViewById<Button>(R.id.btnEnter).setOnClickListener {
            commitSinhala()
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        }

        return view
    }

    private fun appendChar(c: String) {
        buffer.append(c)
        updateComposing()
    }

    private fun updateComposing() {
        val sinhala = convertSinglish(buffer.toString())
        previewText?.text = if (buffer.isEmpty()) "Hima Smart Board • Singlish -> සිංහල" else "${buffer}  →  $sinhala"
        currentInputConnection?.setComposingText(sinhala, 1)
    }

    private fun commitSinhala() {
        if (buffer.isNotEmpty()) {
            val sinhala = convertSinglish(buffer.toString())
            currentInputConnection?.commitText(sinhala, 1)
            buffer.clear()
            previewText?.text = "Hima Smart Board • Singlish -> සිංහල"
        }
    }

    private fun convertSinglish(input: String): String {
        val lower = input.lowercase()
        return when (lower) {
            "amma" -> "අම්මා"
            "ama" -> "අම"
            "ka" -> "ක"
            "kaa" -> "කා"
            "k" -> "ක්"
            "ma" -> "ම"
            "maa" -> "මා"
            "m" -> "ම්"
            "a" -> "අ"
            "aa" -> "ආ"
            else -> lower
        }
    }
}
