package com.kernelbreach.core.database.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Whether content validation errors should abort import (true in debug builds).
 * The :app module binds this from BuildConfig.DEBUG.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ContentFailOnError

/** Module codes marked human-reviewed. The :app module may bind a real set. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ReviewedModuleCodes
