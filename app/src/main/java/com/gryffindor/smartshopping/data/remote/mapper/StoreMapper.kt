package com.gryffindor.smartshopping.data.remote.mapper

import com.gryffindor.smartshopping.data.remote.dto.StoreDto
import com.gryffindor.smartshopping.domain.model.Store

/**
 * Maps StoreDto from Backend to domain Store model.
 */
fun StoreDto.toDomain(): Store = Store(
    id = id,
    name = name,
    brand = brand,
    country = country,
    city = city,
    type = type,
    airportCode = airportCode
)
