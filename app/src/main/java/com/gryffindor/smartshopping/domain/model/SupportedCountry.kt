package com.gryffindor.smartshopping.domain.model

/**
 * Countries supported for currency conversion.
 * Each maps to a currency code sent to the Backend at session creation.
 */
enum class SupportedCountry(
    val currencyCode: String,
    val displayName: String,
    val currencySymbol: String
) {
    USA("USD", "미국", "$"),
    CHINA("CNY", "중국", "¥");
}
