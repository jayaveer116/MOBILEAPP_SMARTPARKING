package com.example.project.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.project.data.model.ParkingLot
import com.example.project.data.model.ParkingSpot
import com.example.project.databinding.DialogSpotPickerBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SpotPickerBottomSheetDialog(
    private val lot: ParkingLot,
    private val spots: List<ParkingSpot>,
    private val onSpotChosen: (ParkingSpot) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogSpotPickerBinding? = null
    private val binding get() = _binding!!

    private var selectedSpot: ParkingSpot? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSpotPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvPickerTitle.text = "Select Spot in ${lot.name}"
        binding.tvPickerSubtitle.text = "${lot.availableSpots} Free Spots • $${lot.hourlyRate}/hr"

        val adapter = SpotAdapter { spot ->
            selectedSpot = spot
            binding.btnConfirmSpotSelection.isEnabled = true
            binding.btnConfirmSpotSelection.text = "Reserve Spot ${spot.spotNumber}"
        }

        binding.rvSpotsGrid.layoutManager = GridLayoutManager(context, 4)
        binding.rvSpotsGrid.adapter = adapter
        adapter.submitList(spots)

        binding.btnClosePicker.setOnClickListener { dismiss() }

        binding.btnConfirmSpotSelection.setOnClickListener {
            selectedSpot?.let { spot ->
                dismiss()
                onSpotChosen(spot)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
