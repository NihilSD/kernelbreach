package com.kernelbreach.app

import android.app.Application
import com.kernelbreach.core.database.repo.CurriculumRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class KernelBreachApplication : Application() {

    @Inject lateinit var curriculumRepository: CurriculumRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Import bundled content on first launch / after an update. Idempotent:
        // a no-op when the content hash is unchanged.
        appScope.launch { curriculumRepository.ensureImported() }
    }
}
