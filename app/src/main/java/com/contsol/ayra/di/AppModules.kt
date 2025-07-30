package com.contsol.ayra.di

import com.contsol.ayra.presentation.main.MainViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

object AppModules {
    private val appModule =
        module {
            viewModel { MainViewModel() }
        }

    private val networkModule =
        module {
//            single<TerbangAjaApiService> { TerbangAjaApiService.invoke(androidContext()) }
        }
    private val localModule =
        module {
//            single<AppDatabase> { AppDatabase.createInstance(androidContext()) }

        }

    private val datasource =
        module {
//            single<AuthDataSource> { AuthDataSourceImpl(get(), get()) }
        }

    private val repository =
        module {
//            single<AuthRepository> { AuthRepositoryImpl(get()) }
        }

    private val viewModelModule =
        module {
//            viewModelOf(::HomeViewModel)
//            viewModel { params ->
//                DetailFavouriteViewModel(
//                    extras = params.get(),
//                )
//            }
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