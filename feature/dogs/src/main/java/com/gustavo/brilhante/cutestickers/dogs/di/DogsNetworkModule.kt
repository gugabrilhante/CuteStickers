package com.gustavo.brilhante.cutestickers.dogs.di

import com.gustavo.brilhante.cutestickers.common.network.DogsApi
import com.gustavo.brilhante.cutestickers.network.MediaService
import com.gustavo.brilhante.cutestickers.dogs.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DogsNetworkModule {

    @Provides
    @Singleton
    @DogsApi
    fun provideDogsService(
        retrofitBuilder: Retrofit.Builder,
        okHttpClient: OkHttpClient
    ): MediaService {
        return retrofitBuilder
            .baseUrl("https://api.thedogapi.com/v1/")
            .client(
                okHttpClient.newBuilder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .addHeader("x-api-key", BuildConfig.DOGS_API_KEY)
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            )
            .build()
            .create(MediaService::class.java)
    }
}
