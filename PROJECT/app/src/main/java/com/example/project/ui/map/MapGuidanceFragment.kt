package com.example.project.ui.map

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.project.data.model.OccupancyStatus
import com.example.project.data.model.ParkingLot
import com.example.project.data.model.UserPreferences
import com.example.project.databinding.FragmentMapGuidanceBinding
import com.example.project.ui.dialogs.CreateReservationDialogFragment
import com.example.project.ui.dialogs.SpotPickerBottomSheetDialog
import com.example.project.ui.viewmodel.ParkingViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MapGuidanceFragment : Fragment() {

    private var _binding: FragmentMapGuidanceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ParkingViewModel by activityViewModels()

    private var userMarker: Marker? = null
    private val lotMarkers = mutableListOf<Marker>()
    private var routePolyline: Polyline? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize OsmDroid Configuration
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE)
        )

        _binding = FragmentMapGuidanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMapView()
        setupListeners()
        observeViewModel()
    }

    private fun setupMapView() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.controller.setZoom(15.0)

        // Default center
        val startPoint = GeoPoint(37.7880, -122.4020)
        binding.mapView.controller.setCenter(startPoint)
    }

    private fun setupListeners() {
        binding.fabMyLocation.setOnClickListener {
            viewModel.userLocation.value?.let { loc ->
                val userPt = GeoPoint(loc.latitude, loc.longitude)
                binding.mapView.controller.animateTo(userPt)
            }
        }

        binding.chipEv.setOnCheckedChangeListener { _, isChecked ->
            updateFilterPreferences { prefs -> prefs.requireEv = isChecked }
        }

        binding.chipCovered.setOnCheckedChangeListener { _, isChecked ->
            updateFilterPreferences { prefs -> prefs.requireCovered = isChecked }
        }

        binding.chipHandicap.setOnCheckedChangeListener { _, isChecked ->
            updateFilterPreferences { prefs -> prefs.requireHandicap = isChecked }
        }
    }

    private fun updateFilterPreferences(block: (UserPreferences) -> Unit) {
        val currentPrefs = viewModel.userPreferences.value ?: UserPreferences()
        block(currentPrefs)
        viewModel.updatePreferences(currentPrefs)
    }

    private fun observeViewModel() {
        viewModel.userLocation.observe(viewLifecycleOwner) { loc ->
            val userPt = GeoPoint(loc.latitude, loc.longitude)
            if (userMarker == null) {
                userMarker = Marker(binding.mapView).apply {
                    position = userPt
                    title = "My GPS Location"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                binding.mapView.overlays.add(userMarker)
            } else {
                userMarker?.position = userPt
            }
            binding.mapView.invalidate()
        }

        viewModel.parkingLots.observe(viewLifecycleOwner) { lots ->
            renderParkingLotMarkers(lots)
        }

        viewModel.selectedLot.observe(viewLifecycleOwner) { selected ->
            if (selected != null) {
                showLotPeekCard(selected)
                drawRouteToLot(selected)
            } else {
                binding.cardLotPeek.visibility = View.GONE
                clearRouteLine()
            }
        }

        viewModel.spotsForSelectedLot.observe(viewLifecycleOwner) { spots ->
            val selectedLot = viewModel.selectedLot.value
            if (selectedLot != null && spots.isNotEmpty()) {
                // Opened when View Spots clicked
            }
        }

        viewModel.statusMessage.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                viewModel.clearStatusMessage()
            }
        }
    }

    private fun renderParkingLotMarkers(lots: List<ParkingLot>) {
        lotMarkers.forEach { binding.mapView.overlays.remove(it) }
        lotMarkers.clear()

        lots.forEach { lot ->
            val marker = Marker(binding.mapView).apply {
                position = GeoPoint(lot.latitude, lot.longitude)
                title = lot.name
                snippet = "${lot.availableSpots} spots free • $${lot.hourlyRate}/hr"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                setOnMarkerClickListener { _, _ ->
                    viewModel.selectParkingLot(lot)
                    true
                }
            }
            lotMarkers.add(marker)
            binding.mapView.overlays.add(marker)
        }

        binding.mapView.invalidate()
    }

    private fun showLotPeekCard(lot: ParkingLot) {
        binding.tvPeekName.text = lot.name
        binding.tvPeekAddress.text = "${lot.address} • ${lot.distanceKm} km (${lot.etaMinutes} mins)"
        binding.tvPeekPrice.text = String.format("$%.2f / hour", lot.hourlyRate)

        binding.tvPeekStatusBadge.text = "${lot.availableSpots} Free"
        when (lot.occupancyStatus) {
            OccupancyStatus.LOW -> {
                binding.tvPeekStatusBadge.text = "${lot.availableSpots} Free (Plenty)"
                binding.tvPeekStatusBadge.setTextColor(Color.parseColor("#137333"))
            }
            OccupancyStatus.MODERATE -> {
                binding.tvPeekStatusBadge.text = "${lot.availableSpots} Free (Moderate)"
                binding.tvPeekStatusBadge.setTextColor(Color.parseColor("#B06000"))
            }
            OccupancyStatus.HIGH, OccupancyStatus.FULL -> {
                binding.tvPeekStatusBadge.text = "${lot.availableSpots} Free (Almost Full)"
                binding.tvPeekStatusBadge.setTextColor(Color.parseColor("#C5221F"))
            }
        }

        binding.cardLotPeek.visibility = View.VISIBLE

        binding.btnPeekViewSpots.setOnClickListener {
            val spots = viewModel.spotsForSelectedLot.value ?: emptyList()
            SpotPickerBottomSheetDialog(lot, spots) { chosenSpot ->
                openReservationDialog(lot, chosenSpot)
            }.show(parentFragmentManager, "SpotPicker")
        }

        binding.btnPeekReserve.setOnClickListener {
            val spots = viewModel.spotsForSelectedLot.value ?: emptyList()
            val firstAvailable = spots.find { it.status == com.example.project.data.model.SpotStatus.AVAILABLE }
                ?: com.example.project.data.model.ParkingSpot(
                    spotId = "${lot.id}_A01", lotId = lot.id, spotNumber = "A-01", pricePerHour = lot.hourlyRate
                )
            openReservationDialog(lot, firstAvailable)
        }
    }

    private fun openReservationDialog(lot: ParkingLot, spot: com.example.project.data.model.ParkingSpot) {
        CreateReservationDialogFragment(lot, spot) { plate, duration ->
            viewModel.createReservation(lot.id, spot.spotNumber, plate, duration)
        }.show(parentFragmentManager, "CreateReservation")
    }

    private fun drawRouteToLot(lot: ParkingLot) {
        clearRouteLine()
        val userLoc = viewModel.userLocation.value ?: return
        val userPt = GeoPoint(userLoc.latitude, userLoc.longitude)
        val destPt = GeoPoint(lot.latitude, lot.longitude)

        routePolyline = Polyline(binding.mapView).apply {
            addPoint(userPt)
            addPoint(GeoPoint((userPt.latitude + destPt.latitude) / 2, userPt.longitude))
            addPoint(destPt)
            outlinePaint.color = Color.parseColor("#1A73E8")
            outlinePaint.strokeWidth = 10f
        }

        binding.mapView.overlays.add(routePolyline)
        binding.mapView.controller.animateTo(destPt)
        binding.mapView.invalidate()
    }

    private fun clearRouteLine() {
        routePolyline?.let { binding.mapView.overlays.remove(it) }
        routePolyline = null
        binding.mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
