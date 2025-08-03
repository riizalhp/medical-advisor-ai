package com.contsol.ayra.di

import com.contsol.ayra.presentation.main.MainViewModel
import android.content.SharedPreferences
import com.contsol.ayra.data.datasource.ActivityDataSource
import com.contsol.ayra.data.datasource.ActivityDataSourceImpl
import com.contsol.ayra.data.repository.ActivityRepository
import com.contsol.ayra.data.repository.ActivityRepositoryImpl
import com.contsol.ayra.data.source.local.preference.ActivityPreference
import com.contsol.ayra.data.source.local.preference.ActivityPreferenceImpl
import com.contsol.ayra.presentation.activity.ActivityViewModel
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
        }

    private val datasource =
        module {
            single<ActivityDataSource> { ActivityDataSourceImpl(get()) }
        }

    private val repository =
        module {
            single<ActivityRepository> { ActivityRepositoryImpl(get()) }
        }

    private val viewModelModule =
        module {
            viewModelOf(::ActivityViewModel)
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