package com.gustavo.brilhante.cutecats.feature.dogs.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gustavo.brilhante.cutecats.core.model.MediaItem

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
