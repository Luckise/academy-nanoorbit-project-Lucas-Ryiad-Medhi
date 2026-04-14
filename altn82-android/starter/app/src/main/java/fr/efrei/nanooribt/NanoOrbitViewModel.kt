package fr.efrei.nanooribt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NanoOrbitViewModel(private val repository: NanoOrbitRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStatut = MutableStateFlow<StatutSatellite?>(null)
    val selectedStatut: StateFlow<StatutSatellite?> = _selectedStatut.asStateFlow()

    private val _allSatellites = repository.getSatellitesFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredSatellites: StateFlow<List<Satellite>> = combine(
        _allSatellites, _searchQuery, _selectedStatut
    ) { satellites, query, statut ->
        satellites.filter { satellite ->
            val matchesQuery = satellite.nomSatellite.contains(query, ignoreCase = true) ||
                    satellite.idOrbite.contains(query, ignoreCase = true)
            val matchesStatut = statut == null || satellite.statut == statut
            matchesQuery && matchesStatut
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fenetres: StateFlow<List<FenetreCom>> = repository.getFenetresFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stations: StateFlow<List<StationSol>> = repository.stations

    private val _instruments = MutableStateFlow<List<Instrument>>(emptyList())
    val instruments: StateFlow<List<Instrument>> = _instruments.asStateFlow()

    private val _missions = MutableStateFlow<List<Mission>>(emptyList())
    val missions: StateFlow<List<Mission>> = _missions.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _isOffline.value = false
            try {
                repository.refreshSatellites()
                repository.refreshFenetres()
                repository.refreshStations()
            } catch (e: Exception) {
                _isOffline.value = true
                _errorMessage.value = "Mode hors-ligne (serveur indisponible)"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadSatelliteDetails(satelliteId: String) {
        viewModelScope.launch {
            try {
                _instruments.value = repository.getInstrumentsForSatellite(satelliteId)
                _missions.value = repository.getMissionsForSatellite(satelliteId)
            } catch (_: Exception) { }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onStatutFilterChange(statut: StatutSatellite?) {
        _selectedStatut.value = statut
    }

    fun addFenetre(fenetre: FenetreCom) {
        viewModelScope.launch {
            try {
                repository.addFenetre(fenetre)
            } catch (_: Exception) { }
        }
    }

    fun loadSatellites() = refreshData()
    fun refreshSatellites() = refreshData()

    class Factory(private val repository: NanoOrbitRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NanoOrbitViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return NanoOrbitViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
