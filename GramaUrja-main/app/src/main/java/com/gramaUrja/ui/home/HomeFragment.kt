package com.gramaUrja.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.gramaUrja.R
import com.gramaUrja.databinding.FragmentHomeBinding
import com.gramaUrja.model.PowerStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.refreshZone()
        setupClickListeners()
        observeViewModel()
        binding.tvZoneName.text = viewModel.selectedZoneName ?: getString(R.string.no_zone_selected)
    }

    private fun setupClickListeners() {
        binding.btnPowerOn.setOnClickListener {
            viewModel.reportStatus("ON")
        }
        binding.btnPowerOff.setOnClickListener {
            viewModel.reportStatus("OFF")
        }
        binding.btnConfirm.setOnClickListener {
            viewModel.confirmStatus()
        }
        binding.btnChangeZone.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_zone)
        }
        binding.cardStatus.setOnClickListener {
            if (viewModel.selectedZoneName == null) {
                findNavController().navigate(R.id.action_home_to_zone)
            }
        }
        binding.fabTimer.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_timer)
        }
        binding.tvHistory.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_history)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.powerStatus.collect { status -> updateStatusUI(status) } }
                launch { viewModel.freshnessInfo.collect { info ->
                    binding.tvLastUpdated.text = info.label
                    binding.tvLastUpdated.setTextColor(info.colorHex.toColorInt())
                }}
                launch { viewModel.toastMessage.collect { msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }}
                launch { viewModel.isLoading.collect { loading ->
                    binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                }}
            }
        }
    }

    private fun updateStatusUI(status: PowerStatus?) {
        if (viewModel.selectedZoneName == null) {
            binding.cardStatus.setCardBackgroundColor("#616161".toColorInt())
            binding.tvStatusMain.text = "?"
            binding.tvStatusLabel.text = getString(R.string.no_zone_selected)
            binding.tvConfirmCount.visibility = View.INVISIBLE
            binding.tvLastUpdated.visibility = View.INVISIBLE
            return
        }

        binding.tvConfirmCount.visibility = View.VISIBLE
        binding.tvLastUpdated.visibility = View.VISIBLE

        if (status == null) {
            binding.cardStatus.setCardBackgroundColor("#616161".toColorInt())
            binding.tvStatusMain.text = "—"
            binding.tvStatusLabel.text = getString(R.string.status_waiting)
            return
        }

        val bgColor: Int
        val statusText: String
        val labelText: String

        when (status.status) {
            "ON" -> {
                bgColor = "#2E7D32".toColorInt()
                statusText = getString(R.string.power_on)
                labelText = getString(R.string.power_available)
            }
            "OFF" -> {
                bgColor = "#C62828".toColorInt()
                statusText = getString(R.string.power_off)
                labelText = getString(R.string.power_unavailable)
            }
            else -> {
                bgColor = "#616161".toColorInt()
                statusText = "UNKNOWN"
                labelText = getString(R.string.status_unknown)
            }
        }

        binding.cardStatus.setCardBackgroundColor(bgColor)
        binding.tvStatusMain.text = statusText
        binding.tvStatusLabel.text = labelText
        binding.tvConfirmCount.text = getString(R.string.confirmed_by_farmers, status.confirmCount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
