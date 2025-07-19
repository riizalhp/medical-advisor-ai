package com.contsol.ayra.di

import org.koin.core.module.Module
import org.koin.dsl.module

object AppModules {
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
            networkModule,
            localModule,
            datasource,
            repository,
            viewModelModule,
        )
}