package com.gustavo.brilhante.cutecats.domain

import com.gustavo.brilhante.cutecats.data.ItemRepository
import javax.inject.Inject
import javax.inject.Named

class ItemsFacade @Inject constructor(
    @Named("CatsItemRepository") private val catItemRepository: ItemRepository,
    @Named("DogsItemRepository") private val dogItemRepository: ItemRepository,
) {
    suspend fun fetchCatItems() = catItemRepository.fetchItems().map { item ->
        Item(
            id = item.id,
            url = item.url,
            with = item.width,
            height = item.height,
        )
    }
    suspend fun fetchDogItems() = dogItemRepository.fetchItems().map { item ->
        Item(
            id = item.id,
            url = item.url,
            with = item.width,
            height = item.height,
        )
    }
}