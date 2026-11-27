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

        val keyMap = mapOf(
            R.id.btnQ to "q", R.id.btnW to "w", R.id.btnE to "e", R.id.btnR to "r",
            R.id.btnT to "t", R.id.btnY to "y", R.id.btnU to "u", R.id.btnI to "i",
            R.id.btnO to "o", R.id.btnP to "p", R.id.btnA to "a", R.id.btnS to "s",
            R.id.btnD to "d", R.id.btnF to "f", R.id.btnG to "g", R.id.btnH to "h",
            R.id.btnJ to "j", R.id.btnK to "k", R.id.btnL to "l", R.id.btnZ to "z",
            R.id.btnX to "x", R.id.btnC to "c", R.id.btnV to "v", R.id.btnB to "b",
            R.id.btnN to "n", R.id.btnM to "m"
        )

        for ((id, letter) in keyMap) {
            view.findViewById<Button?>(id)?.setOnClickListener {
                appendChar(letter)
            }
        }

        view.findViewById<Button?>(R.id.btnDel)?.setOnClickListener {
            if (buffer.isNotEmpty()) {
                buffer.deleteCharAt(buffer.length - 1)
                updateComposing()
            } else {
                currentInputConnection?.deleteSurroundingText(1, 0)
            }
        }

        view.findViewById<Button?>(R.id.btnSpace)?.setOnClickListener {
            commitSinhala()
            currentInputConnection?.commitText(" ", 1)
        }

        view.findViewById<Button?>(R.id.btnEnter)?.setOnClickListener {
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
        previewText?.text = if (buffer.isEmpty()) {
            "Hima Smart Board • Singlish -> සිංහල"
        } else {
            "${buffer}  →  $sinhala"
        }
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
        val words = mapOf(
            "amma" to "අම්මා",
            "thaththa" to "තාත්තා",
            "kohomada" to "කොහොමද",
            "monawada" to "මොනවද",
            "hima" to "හිම",
            "subha" to "සුබ",
            "dawask" to "දවසක්",
            "adare" to "ආදරේ",
            "oya" to "ඔයා",
            "mama" to "මම",
            "api" to "අපි"
        )
        words[lower]?.let { return it }

        val singles = mapOf(
            "a" to "අ", "aa" to "ආ", "ae" to "ඇ", "aee" to "ඈ",
            "i" to "ඉ", "ii" to "ඊ", "u" to "උ", "uu" to "ඌ",
            "e" to "එ", "ee" to "ඒ", "o" to "ඔ", "oo" to "ඕ",
            "ka" to "ක", "kaa" to "කා", "k" to "ක්",
            "ga" to "ග", "gaa" to "ගා", "g" to "ග්",
            "cha" to "ච", "ch" to "ච්",
            "ja" to "ජ", "j" to "ජ්",
            "ta" to "ට", "t" to "ට්",
            "da" to "ඩ", "d" to "ඩ්",
            "na" to "න", "n" to "න්",
            "pa" to "ප", "p" to "ප්",
            "ba" to "බ", "b" to "බ්",
            "ma" to "ම", "maa" to "මා", "m" to "ම්",
            "ya" to "ය", "y" to "ය්",
            "ra" to "ර", "r" to "ර්",
            "la" to "ල", "l" to "ල්",
            "wa" to "ව", "va" to "ව", "w" to "ව්", "v" to "ව්",
            "sa" to "ස", "s" to "ස්",
            "ha" to "හ", "h" to "හ්"
        )
        return singles[lower] ?: lower
    }
}
