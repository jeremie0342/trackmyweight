package com.kps.trackmyweight.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Les repositories sont annotés `@Inject constructor` + `@Singleton`, donc Hilt les
 * fournit automatiquement sans binding explicite. Ce module reste pour futures liaisons
 * (interfaces domain → impls data).
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule
