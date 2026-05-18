package com.gramaUrja.ui.timer

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gramaUrja.GramaUrjaApp
import com.gramaUrja.R
import com.gramaUrja.databinding.FragmentPumpTimerBinding
import com.gramaUrja.model.CropType
import com.gramaUrja.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PumpTimerFragment : Fragment() {

    private var _binding: FragmentPumpTimerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PumpTimerViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPumpTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCropSpinner()
        setupAreaInput()
        setupTimerButtons()
        observeViewModel()
    }

    private fun setupCropSpinner() {
        val crops = CropType.values().map { it.displayName }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, crops)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCrop.adapter = adapter
        binding.spinnerCrop.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                viewModel.setCrop(CropType.values()[pos])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupAreaInput() {
        binding.btnAreaMinus.setOnClickListener {
            val current = binding.etArea.text.toString().toDoubleOrNull() ?: 1.0
            val newVal = (current - 0.5).coerceAtLeast(0.5)
            binding.etArea.setText(newVal.toString())
            viewModel.setArea(newVal)
        }
        binding.btnAreaPlus.setOnClickListener {
            val current = binding.etArea.text.toString().toDoubleOrNull() ?: 1.0
            val newVal = (current + 0.5).coerceAtMost(50.0)
            binding.etArea.setText(newVal.toString())
            viewModel.setArea(newVal)
        }
    }

    private fun setupTimerButtons() {
        binding.btnStartTimer.setOnClickListener {
            viewModel.startTimer()
        }
        binding.btnStopTimer.setOnClickListener {
            viewModel.stopTimer()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.countdownSeconds.collect { secs ->
                    binding.tvCountdown.text = viewModel.formattedCountdown
                    if (secs == 0L && viewModel.isRunning.value == false) {
                        showTimerCompleteNotification()
                    }
                }}
                launch { viewModel.calculatedMinutes.collect { mins ->
                    binding.tvRecommendedTime.text = getString(R.string.recommended_time_format, viewModel.formattedDuration)
                }}
                launch { viewModel.isRunning.collect { running ->
                    binding.btnStartTimer.isEnabled = !running
                    binding.btnStopTimer.isEnabled  = running
                    binding.spinnerCrop.isEnabled   = !running
                }}
            }
        }
    }

    private fun showTimerCompleteNotification() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        val pi = PendingIntent.getActivity(requireContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notif = NotificationCompat.Builder(requireContext(), GramaUrjaApp.CHANNEL_PUMP_TIMER)
            .setSmallIcon(R.drawable.ic_pump)
            .setContentTitle(getString(R.string.timer_complete_title))
            .setContentText(getString(R.string.timer_complete_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        val manager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1002, notif)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
