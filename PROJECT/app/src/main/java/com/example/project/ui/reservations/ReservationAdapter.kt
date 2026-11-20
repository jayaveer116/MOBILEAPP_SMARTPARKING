package com.example.project.ui.reservations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.project.data.model.Reservation
import com.example.project.data.model.ReservationStatus
import com.example.project.databinding.ItemReservationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReservationAdapter(
    private val onCancelClick: (Reservation) -> Unit,
    private val onNavigateClick: (Reservation) -> Unit
) : RecyclerView.Adapter<ReservationAdapter.ViewHolder>() {

    private var items: List<Reservation> = emptyList()

    fun submitList(newList: List<Reservation>) {
        this.items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReservationBinding.inflate(
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

    inner class ViewHolder(private val binding: ItemReservationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(res: Reservation) {
            binding.tvTicketId.text = "Pass ID: ${res.reservationId}"
            binding.tvTicketLotName.text = res.lotName
            binding.tvTicketSpot.text = "Spot: ${res.spotNumber}"
            binding.tvTicketPlate.text = "Vehicle: ${res.vehiclePlate}"
            binding.tvTicketPrice.text = String.format(Locale.US, "Paid: $%.2f (%d Hours)", res.totalPrice, res.durationHours)

            val sdf = SimpleDateFormat("hh:mm a", Locale.US)
            val endTimeStr = sdf.format(Date(res.endTimeMillis))
            binding.tvTicketTime.text = "Valid until $endTimeStr"

            if (res.currentStatus == ReservationStatus.ACTIVE && !res.isExpired) {
                binding.tvTicketStatus.text = "ACTIVE PASS"
                binding.btnCancelTicket.visibility = View.VISIBLE
                binding.btnNavigateTicket.visibility = View.VISIBLE
            } else if (res.currentStatus == ReservationStatus.CANCELLED) {
                binding.tvTicketStatus.text = "CANCELLED"
                binding.btnCancelTicket.visibility = View.GONE
                binding.btnNavigateTicket.visibility = View.GONE
            } else {
                binding.tvTicketStatus.text = "EXPIRED"
                binding.btnCancelTicket.visibility = View.GONE
                binding.btnNavigateTicket.visibility = View.GONE
            }

            // Generate QR Code bitmap
            val qrBitmap = QRCodeGenerator.generateQRCode(res.qrPassCode, 240, 240)
            if (qrBitmap != null) {
                binding.ivTicketQr.setImageBitmap(qrBitmap)
            }

            binding.btnCancelTicket.setOnClickListener { onCancelClick(res) }
            binding.btnNavigateTicket.setOnClickListener { onNavigateClick(res) }
        }
    }
}
