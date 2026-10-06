package com.kernelbreach.app.di

import com.kernelbreach.app.BuildConfig
import com.kernelbreach.core.database.di.ContentFailOnError
import com.kernelbreach.core.database.di.ReviewedModuleCodes
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** Fail loudly on content errors in debug; log-and-skip in release. */
    @Provides
    @ContentFailOnError
    fun provideContentFailOnError(): Boolean = BuildConfig.DEBUG

    /**
     * Module codes a human has fact-checked. Empty for now — all content is
     * AI-drafted and surfaced as unreviewed in the dev screen until this grows.
     */
    @Provides
    @ReviewedModuleCodes
    fun provideReviewedModuleCodes(): Set<String> = emptySet()
}
