package com.kernelbreach.core.database.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.kernelbreach.core.content.ContentSource
import com.kernelbreach.core.database.KernelBreachDatabase
import com.kernelbreach.core.database.content.AssetContentSource
import com.kernelbreach.core.database.content.RoomContentImporter
import com.kernelbreach.core.database.repo.CurriculumRepository
import com.kernelbreach.core.database.repo.ProgressRepository
import com.kernelbreach.core.database.repo.ReviewRepository
import com.kernelbreach.core.database.repo.SettingsRepository
import com.kernelbreach.core.srs.SrsClock
import com.kernelbreach.core.srs.SrsEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KernelBreachDatabase =
        Room.databaseBuilder(context, KernelBreachDatabase::class.java, KernelBreachDatabase.NAME)
            // Content is re-derivable from assets, so a destructive migration during
            // early development is acceptable; replace with real migrations before
            // shipping a schema change that affects user-state tables.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideContentSource(@ApplicationContext context: Context): ContentSource =
        AssetContentSource(context)

    @Provides
    @Singleton
    fun provideSrsClock(): SrsClock = SrsClock.system()

    @Provides
    @Singleton
    fun provideSrsEngine(clock: SrsClock): SrsEngine = SrsEngine(clock)

    @Provides
    @Singleton
    fun provideImporter(
        db: KernelBreachDatabase,
        source: ContentSource,
        @ContentFailOnError failOnError: Boolean,
        @ReviewedModuleCodes reviewedCodes: Set<@JvmSuppressWildcards String>,
        @IoDispatcher io: CoroutineDispatcher,
    ): RoomContentImporter = RoomContentImporter(
        db = db,
        source = source,
        reviewedCodes = reviewedCodes,
        failOnError = failOnError,
        ioDispatcher = io,
    )

    @Provides
    @Singleton
    fun provideCurriculumRepository(
        db: KernelBreachDatabase,
        importer: RoomContentImporter,
        @IoDispatcher io: CoroutineDispatcher,
    ): CurriculumRepository = CurriculumRepository(db, importer, io)

    @Provides
    @Singleton
    fun provideProgressRepository(
        db: KernelBreachDatabase,
        @IoDispatcher io: CoroutineDispatcher,
    ): ProgressRepository = ProgressRepository(db, io)

    @Provides
    @Singleton
    fun provideReviewRepository(
        db: KernelBreachDatabase,
        engine: SrsEngine,
        clock: SrsClock,
        @IoDispatcher io: CoroutineDispatcher,
    ): ReviewRepository = ReviewRepository(db, engine, clock, io)

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context.settingsDataStore)
}
