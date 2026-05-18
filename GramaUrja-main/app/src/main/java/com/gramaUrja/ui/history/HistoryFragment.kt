package com.gramaUrja.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gramaUrja.R
import com.gramaUrja.data.firebase.FirebaseRepository
import com.gramaUrja.databinding.FragmentHistoryBinding
import com.gramaUrja.databinding.ItemHistoryBinding
import com.gramaUrja.model.PowerHistoryEntry
import com.gramaUrja.util.FreshnessUtil
import com.gramaUrja.util.PrefsManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo: FirebaseRepository,
    private val prefs: PrefsManager
) : ViewModel() {

    private val _history = MutableStateFlow<List<PowerHistoryEntry>>(emptyList())
    val history: StateFlow<List<PowerHistoryEntry>> = _history

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { observeHistory() }

    fun observeHistory() {
        val zoneId = prefs.selectedZoneId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            repo.observeHistory(zoneId).collect { entries ->
                _history.value = entries
                _isLoading.value = false
            }
        }
    }
}

// ── Fragment ──────────────────────────────────────────────────────────────────
@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HistoryViewModel by viewModels()
    private val adapter = HistoryAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.history.collect { adapter.submitList(it) } }
                launch { viewModel.isLoading.collect { loading ->
                    binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                }}
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ── Adapter ───────────────────────────────────────────────────────────────────
class HistoryAdapter : ListAdapter<PowerHistoryEntry, HistoryAdapter.ViewHolder>(DiffCallback) {

    private val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    inner class ViewHolder(private val b: ItemHistoryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(entry: PowerHistoryEntry) {
            b.tvStatus.text = entry.status
            b.tvTime.text   = sdf.format(Date(entry.timestamp))
            b.tvReporter.text = b.root.context.getString(R.string.reporter_label, entry.reporterId.take(8))
            val color = if (entry.status == "ON") "#2E7D32" else "#C62828"
            b.viewDot.setBackgroundColor(color.toColorInt())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: ViewHolder, pos: Int) = h.bind(getItem(pos))

    object DiffCallback : DiffUtil.ItemCallback<PowerHistoryEntry>() {
        override fun areItemsTheSame(oldItem: PowerHistoryEntry, newItem: PowerHistoryEntry) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PowerHistoryEntry, newItem: PowerHistoryEntry) = oldItem == newItem
    }
}
