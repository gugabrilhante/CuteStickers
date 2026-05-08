package com.gustavo.brilhante.cutecats.feature.cats.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gustavo.brilhante.cutecats.core.model.MediaItem

@Entity(tableName = "cats")
data class CatEntity(
    @PrimaryKey val id: String,
    val url: String
)

fun CatEntity.asExternalModel() = MediaItem(
    id = id,
    url = url
)

fun MediaItem.asEntity() = CatEntity(
    id = id,
    url = url
)
