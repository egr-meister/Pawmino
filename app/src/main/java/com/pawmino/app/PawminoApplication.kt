package com.pawmino.app

import android.app.Application
import com.pawmino.app.data.PawminoRepository

/**
 * Application class holding the single local repository instance. No services, no background
 * work, no network — just a place to own the DataStore-backed repository.
 */
class PawminoApplication : Application() {
    val repository: PawminoRepository by lazy { PawminoRepository(this) }
}
