package io.github.vlukyanets.lemonflow.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.vlukyanets.lemonflow.data.repo.RoomDayRepository
import io.github.vlukyanets.lemonflow.data.repo.RoomHabitRepository
import io.github.vlukyanets.lemonflow.data.repo.RoomSettingsRepository
import io.github.vlukyanets.lemonflow.data.system.SystemLanguageSetting
import io.github.vlukyanets.lemonflow.domain.port.DayRepository
import io.github.vlukyanets.lemonflow.domain.port.HabitRepository
import io.github.vlukyanets.lemonflow.domain.port.LanguageSetting
import io.github.vlukyanets.lemonflow.domain.port.SettingsRepository

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    fun habitRepository(impl: RoomHabitRepository): HabitRepository

    @Binds
    fun dayRepository(impl: RoomDayRepository): DayRepository

    @Binds
    fun settingsRepository(impl: RoomSettingsRepository): SettingsRepository

    @Binds
    fun languageSetting(impl: SystemLanguageSetting): LanguageSetting
}
