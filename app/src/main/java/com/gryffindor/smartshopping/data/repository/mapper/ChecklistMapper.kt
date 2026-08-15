package com.gryffindor.smartshopping.data.repository.mapper

import com.gryffindor.smartshopping.data.remote.dto.ChecklistItemDto
import com.gryffindor.smartshopping.domain.model.ChecklistItem

fun ChecklistItemDto.toDomain(): ChecklistItem = ChecklistItem(
    id = id,
    title = title,
    description = description,
    required = required
)
