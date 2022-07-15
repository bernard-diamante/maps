package com.bernarddiamante.mymaps.models

import java.io.Serializable

// Represents a map that the user has created


data class UserMap(val title: String, val places: List<Place>): Serializable