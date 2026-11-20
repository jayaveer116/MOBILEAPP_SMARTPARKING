package com.example.project.ui.prediction

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.project.data.model.ParkingLot
import com.example.project.data.model.RiskLevel
import com.example.project.databinding.FragmentOccupancyPredictionBinding
import com.example.project.ui.viewmodel.ParkingViewModel

class OccupancyPredictionFragment : Fragment() {

    private var _binding: FragmentOccupancyPredictionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ParkingViewModel by activityViewModels()
    private val lotsList = mutableListOf<ParkingLot>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOccupancyPredictionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinner()
        observeViewModel()
    }

    private fun setupSpinner() {
        binding.spinnerLots.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in lotsList.indices) {
                    val lot = lotsList[position]
                    viewModel.fetchPredictionForLot(lot)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun observeViewModel() {
        viewModel.parkingLots.observe(viewLifecycleOwner) { lots ->
            if (lots.isNotEmpty()) {
                lotsList.clear()
                lotsList.addAll(lots)

                val lotNames = lots.map { it.name }
                val spinnerAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    lotNames
                )
                binding.spinnerLots.adapter = spinnerAdapter

                if (viewModel.selectedLot.value == null) {
                    viewModel.selectParkingLot(lots.first())
                }
            }
        }

        viewModel.selectedPrediction.observe(viewLifecycleOwner) { pred ->
            if (pred != null) {
                binding.tvPeakHours.text = "Peak Hours: ${pred.peakHoursRange}"
                binding.tvRecommendedArrival.text = "Recommended: ${pred.recommendedArrivalTime}"

                when (pred.riskLevel) {
                    RiskLevel.LOW -> {
                        binding.tvRiskLevel.text = "LOW RISK"
                        binding.tvRiskLevel.setBackgroundResource(com.example.project.R.drawable.bg_badge_green)
                        binding.tvRiskLevel.setTextColor(Color.parseColor("#137333"))
                    }
                    RiskLevel.MODERATE -> {
                        binding.tvRiskLevel.text = "MODERATE RISK"
                        binding.tvRiskLevel.setBackgroundResource(com.example.project.R.drawable.bg_badge_yellow)
                        binding.tvRiskLevel.setTextColor(Color.parseColor("#B06000"))
                    }
                    RiskLevel.HIGH -> {
                        binding.tvRiskLevel.text = "HIGH RISK"
                        binding.tvRiskLevel.setBackgroundResource(com.example.project.R.drawable.bg_badge_red)
                        binding.tvRiskLevel.setTextColor(Color.parseColor("#C5221F"))
                    }
                }

                binding.predictionChartView.setPredictionData(pred.hourlyPredictions)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
