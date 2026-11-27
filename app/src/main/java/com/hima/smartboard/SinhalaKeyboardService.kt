package com.hima.smartboard

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

class SinhalaKeyboardService : InputMethodService() {

    private val buffer = StringBuilder()
    private var previewText: TextView? = null
    private var modeBtn: Button? = null
    private var clipBox: LinearLayout? = null
    private var clipText: TextView? = null
    private var sinhalaStrip: HorizontalScrollView? = null
    private var emojiStrip: HorizontalScrollView? = null
    private var emojiContainer: LinearLayout? = null

    private var isEnglishMode = false
    private val repeatHandler = Handler(Looper.getMainLooper())
    private var isDeletingHeld = false

    private val emojiList = listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "😉",
        "😊", "😇", "🥰", "😍", "🤩", "😘", "😗", "😋", "😛", "😜",
        "🤪", "🤗", "🤭", "🤫", "🤔", "😐", "😑", "😶", "😏", "😒",
        "🙄", "😬", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕", "🤢",
        "🤮", "🥵", "🥶", "🥳", "😎", "🤓", "😕", "😟", "🙁", "☹️",
        "😮", "😯", "😲", "😳", "🥺", "😦", "😧", "😨", "😰", "😢",
        "😭", "😱", "😖", "😣", "😞", "😓", "😩", "😫", "🥱", "😤",
        "😡", "😠", "🤬", "👍", "👎", "👏", "🙌", "👐", "🤝", "🙏",
        "✌️", "🤞", "🤟", "🤘", "👌", "🤏", "👈", "👉", "👆", "👇",
        "☝️", "✋", "🤚", "🖐️", "🖖", "👋", "🤙", "💪", "❤️", "🧡",
        "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❣️", "💕",
        "💞", "💓", "💗", "💖", "💘", "💝", "🔥", "✨", "🌟", "💫",
        "🎉", "🎊", "🎂", "🎁", "📸", "🎥", "🎶", "🎵", "💯", "✅"
    )

    private val deleteRepeatRunnable = object : Runnable {
        override fun run() {
            if (!isDeletingHeld) return
            deleteOneStep()
            repeatHandler.postDelayed(this, 65L)
        }
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        previewText = view.findViewById(R.id.tvPreview)
        modeBtn = view.findViewById(R.id.btnMode)
        clipBox = view.findViewById(R.id.clipBox)
        clipText = view.findViewById(R.id.tvClipText)
        sinhalaStrip = view.findViewById(R.id.sinhalaStrip)
        emojiStrip = view.findViewById(R.id.emojiStrip)
        emojiContainer = view.findViewById(R.id.emojiContainer)

        setupEmojis()

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
                if (isEnglishMode) {
                    currentInputConnection?.commitText(letter, 1)
                } else {
                    appendChar(letter)
                }
            }
        }

        modeBtn?.setOnClickListener {
            if (!isEnglishMode) {
                commitSinhala()
            }
            isEnglishMode = !isEnglishMode
            updateModeUi()
        }

        val pillamMap = mapOf(
            R.id.btnPillaHal to "්",
            R.id.btnPillaAela to "ා",
            R.id.btnPillaKetiIs to "ි",
            R.id.btnPillaDeergaIs to "ී",
            R.id.btnPillaKetiPa to "ු",
            R.id.btnPillaDeergaPa to "ූ",
            R.id.btnPillaKombu to "ෙ",
            R.id.btnPillaDigaKombu to "ේ",
            R.id.btnPillaKombuHaa to "ො",
            R.id.btnPillaDigaKombuHaa to "ෝ",
            R.id.btnBindu to "ං",
            R.id.btnVisarga to "ඃ"
        )
        for ((id, pillam) in pillamMap) {
            view.findViewById<Button?>(id)?.setOnClickListener {
                commitSinhala()
                currentInputConnection?.commitText(pillam, 1)
            }
        }

        view.findViewById<Button?>(R.id.btnEmoji)?.setOnClickListener {
            val showingEmoji = emojiStrip?.visibility == View.VISIBLE
            emojiStrip?.visibility = if (showingEmoji) View.GONE else View.VISIBLE
            sinhalaStrip?.visibility = if (!showingEmoji || isEnglishMode) View.GONE else View.VISIBLE
        }

        view.findViewById<Button?>(R.id.btnPasteClip)?.setOnClickListener {
            val clip = readClipboardText()
            if (!clip.isNullOrEmpty()) {
                commitSinhala()
                currentInputConnection?.commitText(clip, 1)
            }
        }

        view.findViewById<Button?>(R.id.btnDel)?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDeletingHeld = true
                    deleteOneStep()
                    repeatHandler.removeCallbacks(deleteRepeatRunnable)
                    repeatHandler.postDelayed(deleteRepeatRunnable, 350L)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDeletingHeld = false
                    repeatHandler.removeCallbacks(deleteRepeatRunnable)
                    true
                }
                else -> false
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

        updateModeUi()
        refreshClipboardBox()
        return view
    }

    private fun setupEmojis() {
        val container = emojiContainer ?: return
        container.removeAllViews()
        for (emo in emojiList) {
            val btn = Button(this).apply {
                text = emo
                textSize = 18f
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(115, 105)
                setOnClickListener {
                    commitSinhala()
                    currentInputConnection?.commitText(emo, 1)
                }
            }
            container.addView(btn)
        }
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        refreshClipboardBox()
    }

    private fun refreshClipboardBox() {
        val clip = readClipboardText()
        if (!clip.isNullOrBlank()) {
            clipText?.text = "Copied: $clip"
            clipBox?.visibility = View.VISIBLE
        } else {
            clipBox?.visibility = View.GONE
        }
    }

    private fun readClipboardText(): String? {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        if (!cm.hasPrimaryClip()) return null
        val item = cm.primaryClip?.getItemAt(0)
        return item?.coerceToText(this)?.toString()
    }

    private fun updateModeUi() {
        if (isEnglishMode) {
            modeBtn?.text = "EN"
            sinhalaStrip?.visibility = View.GONE
            previewText?.text = "Hima Smart Board • English"
        } else {
            modeBtn?.text = "සිං"
            if (emojiStrip?.visibility != View.VISIBLE) {
                sinhalaStrip?.visibility = View.VISIBLE
            }
            previewText?.text = "Hima Smart Board • සිංහල (Singlish)"
        }
    }

    private fun deleteOneStep() {
        if (!isEnglishMode && buffer.isNotEmpty()) {
            buffer.deleteCharAt(buffer.length - 1)
            updateComposing()
        } else {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
    }

    private fun appendChar(c: String) {
        buffer.append(c)
        updateComposing()
    }

    private fun updateComposing() {
        val sinhala = convertSinglish(buffer.toString())
        previewText?.text = if (buffer.isEmpty()) {
            "Hima Smart Board • සිංහල (Singlish)"
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
            if (!isEnglishMode) {
                previewText?.text = "Hima Smart Board • සිංහල (Singlish)"
            }
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
