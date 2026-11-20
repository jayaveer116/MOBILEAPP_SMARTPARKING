package com.example.project.ui.recommendation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.project.data.model.ParkingLot
import com.example.project.databinding.ItemRecommendationBinding

class RecommendationAdapter(
    private val onReserveClick: (ParkingLot) -> Unit
) : RecyclerView.Adapter<RecommendationAdapter.ViewHolder>() {

    private var items: List<ParkingLot> = emptyList()

    fun submitList(newList: List<ParkingLot>) {
        this.items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecommendationBinding.inflate(
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

    inner class ViewHolder(private val binding: ItemRecommendationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(lot: ParkingLot) {
            binding.tvRecScore.text = "${lot.recommendationScore}% Match"
            binding.tvRecName.text = lot.name
            binding.tvRecReason.text = "💡 ${lot.displayMatchReason}"
            binding.tvRecDetails.text = String.format(
                "$%.2f/hr • %d free spots • %.1f km",
                lot.hourlyRate,
                lot.availableSpots,
                lot.distanceKm
            )

            binding.btnRecReserve.setOnClickListener {
                onReserveClick(lot)
            }
        }
    }
}
