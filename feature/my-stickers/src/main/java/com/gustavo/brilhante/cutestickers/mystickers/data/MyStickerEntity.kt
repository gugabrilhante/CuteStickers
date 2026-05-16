package com.gustavo.brilhante.cutestickers.mystickers.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gustavo.brilhante.cutestickers.model.MediaType
import com.gustavo.brilhante.cutestickers.mystickers.domain.MySticker
import com.gustavo.brilhante.cutestickers.mystickers.domain.SourceType

@Entity(tableName = "my_stickers")
data class MyStickerEntity(
    @PrimaryKey val id: String,
    val localPath: String,
    val sourceType: String,
    val createdAt: Long,
    val mediaType: String = "Static"
)

fun MyStickerEntity.toDomain() = MySticker(
    id = id,
    localPath = localPath,
    sourceType = SourceType.valueOf(sourceType),
    createdAt = createdAt,
    mediaType = if (mediaType == "Animated") MediaType.Animated else MediaType.Static
)

fun MySticker.toEntity() = MyStickerEntity(
    id = id,
    localPath = localPath,
    sourceType = sourceType.name,
    createdAt = createdAt,
    mediaType = if (mediaType is MediaType.Animated) "Animated" else "Static"
)
