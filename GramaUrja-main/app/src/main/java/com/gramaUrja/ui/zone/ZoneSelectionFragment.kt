package com.gramaUrja.ui.zone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gramaUrja.R
import com.gramaUrja.databinding.FragmentZoneSelectionBinding
import com.gramaUrja.databinding.ItemZoneBinding
import com.gramaUrja.model.Zone
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ZoneSelectionFragment : Fragment() {

    private var _binding: FragmentZoneSelectionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ZoneViewModel by viewModels()
    private val adapter = ZoneAdapter { zone -> onZoneSelected(zone) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentZoneSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvZones.layoutManager = LinearLayoutManager(requireContext())
        binding.rvZones.adapter = adapter

        binding.btnSeedData.setOnClickListener {
            viewModel.seedData()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.zones.collect { adapter.submitList(it) } }
                launch { viewModel.isLoading.collect { loading ->
                    binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                }}
                launch { viewModel.toastMessage.collect { msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }}
            }
        }
    }

    private fun onZoneSelected(zone: Zone) {
        viewModel.selectZone(zone)
        findNavController().navigateUp()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ── Adapter ───────────────────────────────────────────────────────────────────
class ZoneAdapter(private val onClick: (Zone) -> Unit) :
    ListAdapter<Zone, ZoneAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val b: ItemZoneBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(zone: Zone) {
            b.tvZoneName.text = zone.name
            b.tvZoneDetail.text = b.root.context.getString(R.string.zone_detail_format, zone.district, zone.taluk)
            b.root.setOnClickListener { onClick(zone) }
            b.root.contentDescription = b.root.context.getString(R.string.desc_zone_item, zone.name)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemZoneBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    object DiffCallback : DiffUtil.ItemCallback<Zone>() {
        override fun areItemsTheSame(oldItem: Zone, newItem: Zone) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Zone, newItem: Zone) = oldItem == newItem
    }
}
