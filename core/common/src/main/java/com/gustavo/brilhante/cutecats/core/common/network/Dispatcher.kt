package com.gustavo.brilhante.cutecats.core.common.network

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: CatsDispatchers)

enum class CatsDispatchers {
    IO,
    Default,
}
