package com.example.project.ui.reservations

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.project.databinding.FragmentReservationsBinding
import com.example.project.ui.viewmodel.ParkingViewModel

class ReservationsFragment : Fragment() {

    private var _binding: FragmentReservationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ParkingViewModel by activityViewModels()
    private lateinit var adapter: ReservationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReservationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ReservationAdapter(
            onCancelClick = { res ->
                viewModel.cancelReservation(res.reservationId)
            },
            onNavigateClick = { res ->
                val gmmIntentUri = Uri.parse("google.navigation:q=${res.latitude},${res.longitude}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${res.latitude},${res.longitude}?q=${Uri.encode(res.lotName)}"))
                    startActivity(genericIntent)
                }
            }
        )

        binding.rvReservations.layoutManager = LinearLayoutManager(context)
        binding.rvReservations.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.userReservations.observe(viewLifecycleOwner) { reservations ->
            if (reservations.isNullOrEmpty()) {
                binding.tvEmptyReservations.visibility = View.VISIBLE
                binding.rvReservations.visibility = View.GONE
            } else {
                binding.tvEmptyReservations.visibility = View.GONE
                binding.rvReservations.visibility = View.VISIBLE
                adapter.submitList(reservations)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarReservations.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
