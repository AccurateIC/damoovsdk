package com.example.accuratedamoov.model

import com.example.accuratedamoov.data.model.TripData

sealed  class FeedUiState {

    object Loading : FeedUiState()
    data class Success(val trips: List<TripData>) : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}