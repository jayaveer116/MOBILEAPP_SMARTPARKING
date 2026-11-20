package com.example.project.ui.dialogs

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.data.model.ParkingSpot
import com.example.project.data.model.SpotStatus
import com.example.project.databinding.ItemParkingSpotBinding

class SpotAdapter(
    private val onSpotSelected: (ParkingSpot) -> Unit
) : RecyclerView.Adapter<SpotAdapter.ViewHolder>() {

    private var items: List<ParkingSpot> = emptyList()
    private var selectedSpotId: String? = null

    fun submitList(newList: List<ParkingSpot>) {
        this.items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemParkingSpotBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemParkingSpotBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(spot: ParkingSpot) {
            binding.tvSpotNumber.text = spot.spotNumber
            binding.tvSpotType.text = when {
                spot.isEvSpot -> "⚡ EV"
                spot.isHandicap -> "♿ Access"
                else -> "Section ${spot.section}"
            }

            val isSelected = spot.spotId == selectedSpotId

            when (spot.currentStatus) {
                SpotStatus.AVAILABLE -> {
                    if (isSelected) {
                        binding.cardSpotContainer.setCardBackgroundColor(Color.parseColor("#1A73E8"))
                        binding.tvSpotNumber.setTextColor(Color.WHITE)
                        binding.tvSpotType.setTextColor(Color.WHITE)
                    } else if (spot.isEvSpot) {
                        binding.cardSpotContainer.setCardBackgroundColor(Color.parseColor("#E8F0FE"))
                        binding.tvSpotNumber.setTextColor(Color.parseColor("#1A73E8"))
                        binding.tvSpotType.setTextColor(Color.parseColor("#1A73E8"))
                    } else {
                        binding.cardSpotContainer.setCardBackgroundColor(Color.parseColor("#E6F4EA"))
                        binding.tvSpotNumber.setTextColor(Color.parseColor("#137333"))
                        binding.tvSpotType.setTextColor(Color.parseColor("#137333"))
                    }
                    binding.cardSpotContainer.isEnabled = true
                }
                SpotStatus.OCCUPIED -> {
                    binding.cardSpotContainer.setCardBackgroundColor(Color.parseColor("#FCE8E6"))
                    binding.tvSpotNumber.setTextColor(Color.parseColor("#C5221F"))
                    binding.tvSpotType.setTextColor(Color.parseColor("#C5221F"))
                    binding.cardSpotContainer.isEnabled = false
                }
                SpotStatus.RESERVED -> {
                    binding.cardSpotContainer.setCardBackgroundColor(Color.parseColor("#FEF7E0"))
                    binding.tvSpotNumber.setTextColor(Color.parseColor("#B06000"))
                    binding.tvSpotType.setTextColor(Color.parseColor("#B06000"))
                    binding.cardSpotContainer.isEnabled = false
                }
                else -> {
                    binding.cardSpotContainer.isEnabled = false
                }
            }

            binding.cardSpotContainer.setOnClickListener {
                if (spot.currentStatus == SpotStatus.AVAILABLE) {
                    selectedSpotId = spot.spotId
                    notifyDataSetChanged()
                    onSpotSelected(spot)
                }
            }
        }
    }
}
