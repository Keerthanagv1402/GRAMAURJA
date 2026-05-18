package com.gramaUrja.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.gramaUrja.R
import com.gramaUrja.databinding.ActivityOnboardingBinding
import com.gramaUrja.databinding.ItemOnboardingBinding
import com.gramaUrja.util.PrefsManager

data class OnboardingPage(val title: String, val description: String, val iconRes: Int)

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var prefs: PrefsManager

    private val pages by lazy {
        listOf(
            OnboardingPage(
                getString(R.string.onboard_title_1),
                getString(R.string.onboard_desc_1),
                R.drawable.ic_zone
            ),
            OnboardingPage(
                getString(R.string.onboard_title_2),
                getString(R.string.onboard_desc_2),
                R.drawable.ic_power
            ),
            OnboardingPage(
                getString(R.string.onboard_title_3),
                getString(R.string.onboard_desc_3),
                R.drawable.ic_notification
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PrefsManager(applicationContext)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = OnboardingAdapter(pages)
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.dotsIndicator, binding.viewPager) { _, _ -> }.attach()

        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < pages.size - 1) {
                binding.viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }

        binding.btnSkip.setOnClickListener { finishOnboarding() }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.btnNext.text = if (position == pages.size - 1) getString(R.string.onboard_get_started) else getString(R.string.onboard_next)
                binding.btnSkip.visibility = if (position == pages.size - 1) View.GONE else View.VISIBLE
            }
        })
    }

    private fun finishOnboarding() {
        prefs.onboardingShown = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

class OnboardingAdapter(private val pages: List<OnboardingPage>) :
    RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {

    class ViewHolder(private val b: ItemOnboardingBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(page: OnboardingPage) {
            b.tvTitle.text       = page.title
            b.tvDescription.text = page.description
            b.ivIcon.setImageResource(page.iconRes)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemOnboardingBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: ViewHolder, pos: Int) = h.bind(pages[pos])
    override fun getItemCount() = pages.size
}
