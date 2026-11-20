package com.example.project.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.project.data.model.ParkingLot
import com.example.project.data.model.ParkingSpot
import com.example.project.databinding.DialogCreateReservationBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale

class CreateReservationDialogFragment(
    private val lot: ParkingLot,
    private val spot: ParkingSpot,
    private val onConfirmBooking: (String, Int) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogCreateReservationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateReservationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvBookingSpotInfo.text = "Spot ${spot.spotNumber} • ${lot.name}"
        binding.tvSummaryRate.text = String.format(Locale.US, "$%.2f / hr", lot.hourlyRate)

        updatePriceCalculation(2)

        binding.sliderDuration.addOnChangeListener { _, value, _ ->
            val durationHours = value.toInt()
            updatePriceCalculation(durationHours)
        }

        binding.btnPayConfirm.setOnClickListener {
            val plate = binding.etLicensePlate.text.toString().trim()
            val duration = binding.sliderDuration.value.toInt()
            if (plate.isNotEmpty()) {
                dismiss()
                onConfirmBooking(plate, duration)
            } else {
                binding.etLicensePlate.error = "Please enter vehicle plate number"
            }
        }
    }

    private fun updatePriceCalculation(durationHours: Int) {
        val total = lot.hourlyRate * durationHours
        binding.tvDurationLabel.text = "Duration: $durationHours Hour${if (durationHours > 1) "s" else ""}"
        binding.tvSummaryTotal.text = String.format(Locale.US, "$%.2f", total)
        binding.btnPayConfirm.text = String.format(Locale.US, "Confirm & Pay $%.2f", total)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
