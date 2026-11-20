package com.example.project.ui.recommendation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project.R
import com.example.project.data.model.UserPreferences
import com.example.project.databinding.FragmentSmartRecommenderBinding
import com.example.project.ui.dialogs.CreateReservationDialogFragment
import com.example.project.ui.dialogs.SpotPickerBottomSheetDialog
import com.example.project.ui.viewmodel.ParkingViewModel

class SmartRecommenderFragment : Fragment() {

    private var _binding: FragmentSmartRecommenderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ParkingViewModel by activityViewModels()
    private lateinit var adapter: RecommendationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSmartRecommenderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = RecommendationAdapter { lot ->
            viewModel.selectParkingLot(lot)
            val spots = viewModel.spotsForSelectedLot.value ?: emptyList()
            SpotPickerBottomSheetDialog(lot, spots) { chosenSpot ->
                CreateReservationDialogFragment(lot, chosenSpot) { plate, duration ->
                    viewModel.createReservation(lot.id, chosenSpot.spotNumber, plate, duration)
                }.show(parentFragmentManager, "CreateReservation")
            }.show(parentFragmentManager, "SpotPicker")
        }

        binding.rvRecommendations.layoutManager = LinearLayoutManager(context)
        binding.rvRecommendations.adapter = adapter
    }

    private fun setupListeners() {
        binding.toggleArrivalTime.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val offsetHours = when (checkedId) {
                    R.id.btn_time_1h -> 1
                    R.id.btn_time_2h -> 2
                    else -> 0
                }
                updatePrefs { prefs -> prefs.targetArrivalTimeOffsetHours = offsetHours }
            }
        }

        binding.switchEv.setOnCheckedChangeListener { _, isChecked ->
            updatePrefs { prefs -> prefs.requireEv = isChecked }
        }

        binding.switchCovered.setOnCheckedChangeListener { _, isChecked ->
            updatePrefs { prefs -> prefs.requireCovered = isChecked }
        }
    }

    private fun updatePrefs(block: (UserPreferences) -> Unit) {
        val prefs = viewModel.userPreferences.value ?: UserPreferences()
        block(prefs)
        viewModel.updatePreferences(prefs)
    }

    private fun observeViewModel() {
        viewModel.recommendedLots.observe(viewLifecycleOwner) { recommendations ->
            adapter.submitList(recommendations)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
