package com.contsol.ayra.di

import com.contsol.ayra.presentation.main.MainViewModel
import android.content.SharedPreferences
import com.contsol.ayra.data.datasource.ActivityDataSource
import com.contsol.ayra.data.datasource.ActivityDataSourceImpl
import com.contsol.ayra.data.datasource.NewUserDataSource
import com.contsol.ayra.data.datasource.NewUserDataSourceImpl
import com.contsol.ayra.data.repository.ActivityRepository
import com.contsol.ayra.data.repository.ActivityRepositoryImpl
import com.contsol.ayra.data.repository.NewUserRepository
import com.contsol.ayra.data.repository.NewUserRepositoryImpl
import com.contsol.ayra.data.source.local.preference.ActivityPreference
import com.contsol.ayra.data.source.local.preference.ActivityPreferenceImpl
import com.contsol.ayra.data.source.local.preference.NewUserPreferences
import com.contsol.ayra.data.source.local.preference.NewUserPreferencesImpl
import com.contsol.ayra.presentation.activity.ActivityViewModel
import com.contsol.ayra.presentation.launcher.LauncherViewModel
import com.contsol.ayra.presentation.onboarding.OnBoardingViewModel
import com.contsol.ayra.utils.SharedPreferenceUtils
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

object AppModules {
    private val appModule =
        module {
            viewModel { MainViewModel() }
        }

    private val networkModule =
        module {
        }
    private val localModule =
        module {
            single<SharedPreferences> {
                SharedPreferenceUtils.createPreference(
                    androidContext(),
                    ActivityPreferenceImpl.PREF_NAME,
                )
            }
            single<ActivityPreference> { ActivityPreferenceImpl(get()) }
            single<NewUserPreferences> { NewUserPreferencesImpl(get()) }
        }

    private val datasource =
        module {
            single<ActivityDataSource> { ActivityDataSourceImpl(get()) }
            single<NewUserDataSource> { NewUserDataSourceImpl(get()) }
        }

    private val repository =
        module {
            single<ActivityRepository> { ActivityRepositoryImpl(get()) }
            single<NewUserRepository> { NewUserRepositoryImpl(get()) }
        }

    private val viewModelModule =
        module {
            viewModelOf(::ActivityViewModel)
            viewModelOf(::OnBoardingViewModel)
            viewModelOf(::LauncherViewModel)
        }

    val modules =
        listOf<Module>(
            appModule,
            networkModule,
            localModule,
            datasource,
            repository,
            viewModelModule,
        )
}