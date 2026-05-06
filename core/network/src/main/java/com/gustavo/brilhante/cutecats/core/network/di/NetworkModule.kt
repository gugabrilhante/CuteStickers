package com.gustavo.brilhante.cutecats.core.network.di

import com.gustavo.brilhante.cutecats.core.common.network.CatsApi
import com.gustavo.brilhante.cutecats.core.common.network.DogsApi
import com.gustavo.brilhante.cutecats.core.network.AnimalService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun providesNetworkJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .build()

    @Provides
    @Singleton
    @CatsApi
    fun provideCatsService(okHttpClient: OkHttpClient, networkJson: Json): AnimalService {
        return Retrofit.Builder()
            .baseUrl("https://api.thecatapi.com/v1/")
            .client(
                okHttpClient.newBuilder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .addHeader("x-api-key", "live_Z1MSgRQkrHWgtPqkhdosyqSC60DFTgLDdt7eKIeDzsv2LbBBbetcb9PJ2N9XIXUp")
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            )
            .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AnimalService::class.java)
    }

    @Provides
    @Singleton
    @DogsApi
    fun provideDogsService(okHttpClient: OkHttpClient, networkJson: Json): AnimalService {
        return Retrofit.Builder()
            .baseUrl("https://api.thedogapi.com/v1/")
            .client(
                okHttpClient.newBuilder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .addHeader("x-api-key", "live_ED5kzKiRg2PhYy3M1TWpoYC1VBQ08gaPztitaaHLxO94pmTKzbNsxY9PLcaTJloc")
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            )
            .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AnimalService::class.java)
    }
}
