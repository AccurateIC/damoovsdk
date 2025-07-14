package com.example.accuratedamoov.ui.dashboard

import android.animation.ValueAnimator
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import com.example.accuratedamoov.R

class DashboardFragment : Fragment() {

    private lateinit var scoreText: TextView
    private lateinit var scoreProgressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        scoreText = view.findViewById(R.id.scoreText)
        scoreProgressBar = view.findViewById(R.id.scoreProgressBar)

        // Animate score (mock score: 78)
        animateScore(78)
    }

    private fun animateScore(targetScore: Int) {
        val animator = ValueAnimator.ofInt(0, targetScore)
        animator.duration = 1000
        animator.interpolator = DecelerateInterpolator()

        animator.addUpdateListener {
            val value = it.animatedValue as Int
            scoreProgressBar.progress = value
            scoreText.text = "Score: $value"
        }

        animator.start()
    }
}
