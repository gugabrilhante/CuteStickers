package com.gustavo.brilhante.cutestickers.dogs.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gustavo.brilhante.cutestickers.model.MediaItem

@Entity(tableName = "dogs")
data class DogEntity(
    @PrimaryKey val id: String,
    val url: String
)

fun DogEntity.asExternalModel() = MediaItem(
    id = id,
    url = url
)

fun MediaItem.asEntity() = DogEntity(
    id = id,
    url = url
)
