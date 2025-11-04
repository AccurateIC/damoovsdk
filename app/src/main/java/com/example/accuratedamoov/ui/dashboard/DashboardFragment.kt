package com.example.accuratedamoov.ui.dashboard

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import pl.droidsonroids.gif.GifDrawable
import com.example.accuratedamoov.R


class DashboardFragment : Fragment() {



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textView = view.findViewById<TextView>(R.id.titleText)
        val text = textView.text.toString()
        val gifDrawable = GifDrawable(resources, R.drawable.ic_cowboy_animated)
        gifDrawable.setBounds(0, 0, 80, 80) // size in px

        val span = SpannableString("$text ")
        val imageSpan = ImageSpan(gifDrawable, ImageSpan.ALIGN_BASELINE)
        span.setSpan(imageSpan, text.length, text.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = span
        textView.movementMethod = LinkMovementMethod.getInstance()


        gifDrawable.callback = object : Drawable.Callback {
            override fun invalidateDrawable(who: Drawable) {
                textView.invalidate()
            }

            override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                textView.postDelayed(what, `when` - SystemClock.uptimeMillis())
            }

            override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                textView.removeCallbacks(what)
            }
        }

        // Toggle buttons setup
        val leftText = view.findViewById<TextView>(R.id.leftBottomText)
        val rightText = view.findViewById<TextView>(R.id.rightBottomText)

        fun select(leftSelected: Boolean) {
            if (leftSelected) {
                leftText.setBackgroundResource(R.drawable.toggle_selected)
                leftText.setTextColor(Color.BLACK)
                rightText.setBackgroundResource(R.drawable.toggle_unselected)
                rightText.setTextColor(Color.WHITE)
            } else {
                rightText.setBackgroundResource(R.drawable.toggle_selected)
                rightText.setTextColor(Color.BLACK)
                leftText.setBackgroundResource(R.drawable.toggle_unselected)
                leftText.setTextColor(Color.WHITE)
            }
        }

        select(true)
        leftText.setOnClickListener { select(true) }
        rightText.setOnClickListener { select(false) }





    }



    override fun onDestroyView() {
        super.onDestroyView()
    }
}

