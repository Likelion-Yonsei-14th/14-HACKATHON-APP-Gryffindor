package com.gryffindor.smartshopping.data.repository.mapper

import com.gryffindor.smartshopping.data.remote.dto.FeedRecommendationResponseDto
import com.gryffindor.smartshopping.data.remote.dto.FeedStoreResponseDto
import com.gryffindor.smartshopping.data.remote.dto.FlightResponseDto
import com.gryffindor.smartshopping.data.remote.dto.HotelStayResponseDto
import com.gryffindor.smartshopping.data.remote.dto.MyPageResponseDto
import com.gryffindor.smartshopping.data.remote.dto.ProductDto
import com.gryffindor.smartshopping.data.remote.dto.PurchaseItemResponseDto
import com.gryffindor.smartshopping.data.remote.dto.PurchaseResponseDto
import com.gryffindor.smartshopping.data.remote.dto.PurchasedProductResponseDto
import com.gryffindor.smartshopping.data.remote.dto.ReceiptItemResponseDto
import com.gryffindor.smartshopping.data.remote.dto.ReceiptResponseDto
import com.gryffindor.smartshopping.data.remote.dto.StoreWishlistProductResponseDto
import com.gryffindor.smartshopping.data.remote.dto.TripDetailResponseDto
import com.gryffindor.smartshopping.data.remote.dto.TripFeedResponseDto
import com.gryffindor.smartshopping.data.remote.dto.TripResponseDto
import com.gryffindor.smartshopping.data.remote.dto.TripSummaryResponseDto
import com.gryffindor.smartshopping.data.remote.dto.VisitReservationResponseDto
import com.gryffindor.smartshopping.domain.model.DemoUser
import com.gryffindor.smartshopping.domain.model.FeedRecommendation
import com.gryffindor.smartshopping.domain.model.Flight
import com.gryffindor.smartshopping.domain.model.HotelStay
import com.gryffindor.smartshopping.domain.model.MyPage
import com.gryffindor.smartshopping.domain.model.Product
import com.gryffindor.smartshopping.domain.model.Purchase
import com.gryffindor.smartshopping.domain.model.PurchaseItem
import com.gryffindor.smartshopping.domain.model.PurchasedProduct
import com.gryffindor.smartshopping.domain.model.Receipt
import com.gryffindor.smartshopping.domain.model.ReceiptItem
import com.gryffindor.smartshopping.domain.model.RecommendedStore
import com.gryffindor.smartshopping.domain.model.ReservationStatus
import com.gryffindor.smartshopping.domain.model.StoreWishlistProduct
import com.gryffindor.smartshopping.domain.model.Trip
import com.gryffindor.smartshopping.domain.model.TripDetail
import com.gryffindor.smartshopping.domain.model.TripFeed
import com.gryffindor.smartshopping.domain.model.TripSummary
import com.gryffindor.smartshopping.domain.model.VisitReservation

// --- Product (reuses existing mapper logic but provides standalone version) ---

fun ProductDto.toProduct(): Product = Product(
    productId = productId,
    sku = sku,
    brand = brand,
    name = name,
    category = category,
    imageUrl = imageUrl
)

// --- MyPage ---

fun MyPageResponseDto.toDomain(): MyPage = MyPage(
    user = DemoUser(id = user.id, name = user.name),
    wishlist = wishlist.map { it.toProduct() },
    purchasedProducts = purchasedProducts.map { it.toDomain() },
    flight = flight?.toDomain(),
    trips = trips.map { it.toDomain() }
)

fun PurchasedProductResponseDto.toDomain(): PurchasedProduct = PurchasedProduct(
    purchaseItemId = purchaseItemId,
    product = product?.toProduct(),
    fallbackProductName = fallbackProductName,
    quantity = quantity,
    price = price,
    currency = currency,
    storeName = storeName,
    purchasedAt = purchasedAt
)

fun TripSummaryResponseDto.toDomain(): TripSummary = TripSummary(
    id = id,
    title = title,
    startsAt = startsAt,
    endsAt = endsAt
)

// --- Flight ---

fun FlightResponseDto.toDomain(): Flight = Flight(
    id = id,
    tripId = tripId,
    departureAirport = departureAirport,
    arrivalAirport = arrivalAirport,
    terminal = terminal,
    flightNumber = flightNumber,
    departureAt = departureAt,
    arrivalAt = arrivalAt,
    airportArrivalAt = airportArrivalAt,
    createdAt = createdAt
)

// --- Receipt ---

fun ReceiptResponseDto.toDomain(): Receipt = Receipt(
    id = id,
    storeName = storeName,
    purchasedAt = purchasedAt,
    totalAmount = totalAmount,
    currency = currency,
    items = items.map { it.toDomain() },
    createdAt = createdAt
)

fun ReceiptItemResponseDto.toDomain(): ReceiptItem = ReceiptItem(
    name = name,
    productId = productId,
    quantity = quantity,
    price = price
)

// --- Purchase ---

fun PurchaseResponseDto.toDomain(): Purchase = Purchase(
    id = id,
    storeName = storeName,
    purchasedAt = purchasedAt,
    totalAmount = totalAmount,
    currency = currency,
    items = items.map { it.toDomain() },
    createdAt = createdAt
)

fun PurchaseItemResponseDto.toDomain(): PurchaseItem = PurchaseItem(
    purchaseItemId = purchaseItemId,
    product = product?.toProduct(),
    fallbackProductName = fallbackProductName,
    quantity = quantity,
    price = price
)

// --- Trip ---

fun TripResponseDto.toDomain(): Trip = Trip(
    id = id,
    title = title,
    destinationCity = destinationCity,
    destinationCountry = destinationCountry,
    startsAt = startsAt,
    endsAt = endsAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TripDetailResponseDto.toDomain(): TripDetail = TripDetail(
    trip = trip.toDomain(),
    flights = flights.map { it.toDomain() },
    hotel = hotel?.toDomain(),
    visitReservations = visitReservations.map { it.toDomain() }
)

// --- Hotel ---

fun HotelStayResponseDto.toDomain(): HotelStay = HotelStay(
    id = id,
    tripId = tripId,
    name = name,
    address = address,
    latitude = latitude,
    longitude = longitude,
    checkInAt = checkInAt,
    checkOutAt = checkOutAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// --- Feed ---

fun TripFeedResponseDto.toDomain(): TripFeed = TripFeed(
    tripId = trip.id,
    tripTitle = trip.title,
    recommendations = recommendations.map { it.toDomain() }
)

fun FeedRecommendationResponseDto.toDomain(): FeedRecommendation = FeedRecommendation(
    product = product.toProduct(),
    reason = reason,
    stores = stores.map { it.toDomain() }
)

fun FeedStoreResponseDto.toDomain(): RecommendedStore = RecommendedStore(
    storeId = storeId,
    name = name,
    type = type,
    distanceFromHotelKm = distanceFromHotelKm,
    airportCode = airportCode,
    terminal = terminal,
    hasWishlistItems = hasWishlistItems,
    reason = reason
)

// --- Store Wishlist ---

fun StoreWishlistProductResponseDto.toDomain(): StoreWishlistProduct = StoreWishlistProduct(
    productId = productId,
    name = name
)

// --- Visit Reservation ---

fun VisitReservationResponseDto.toDomain(): VisitReservation = VisitReservation(
    id = id,
    tripId = tripId,
    storeId = store.storeId,
    storeName = store.name,
    scheduledAt = scheduledAt,
    products = products.map { it.toProduct() },
    status = ReservationStatus.valueOf(status),
    createdAt = createdAt
)
