package com.aliah.quotevomit

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

/** Simple screen to test quotes and explain how to add the widget. */
class MainActivity : Activity() {

    private lateinit var quoteText: TextView
    private lateinit var quoteAuthor: TextView
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        quoteText = findViewById(R.id.main_quote)
        quoteAuthor = findViewById(R.id.main_author)
        button = findViewById(R.id.main_button)

        show(QuoteRepository.load(this))
        QuoteScheduler.scheduleHourly(this)

        button.setOnClickListener {
            button.isEnabled = false
            button.text = getString(R.string.loading)
            Thread {
                val q = QuoteRepository.refresh(applicationContext)
                QuoteWidget.updateAll(applicationContext)
                runOnUiThread {
                    show(q)
                    button.isEnabled = true
                    button.text = getString(R.string.button_vomit)
                }
            }.start()
        }
    }

    private fun show(q: Quote) {
        findViewById<TextView>(R.id.main_label).text = q.label
        quoteText.text = QuoteWidget.displayText(q)
        quoteAuthor.text = if (q.author.isBlank()) "" else "— ${q.author}"
        quoteAuthor.visibility = if (q.author.isBlank()) View.GONE else View.VISIBLE
        val bg = findViewById<ImageView>(R.id.main_bg)
        val photo = BackgroundRepository.loadPhoto(this)
        if (photo != null) bg.setImageBitmap(photo)
        else bg.setImageResource(BackgroundRepository.gradientRes(this))
    }
}
