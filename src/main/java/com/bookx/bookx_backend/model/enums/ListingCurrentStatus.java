package com.bookx.bookx_backend.model.enums;

public enum ListingCurrentStatus {
    AVAILABLE,      // Listing is active and open for offers/requests
    REQUESTED,      // An offer/request has been made and is pending lister's action
    RESERVED,       // Lister has accepted an offer, pending completion (e.g., payment, pickup)
    SOLD,           // Transaction completed for a FOR_SALE listing
    LENT,           // Item is currently lent out for a FOR_LEND listing
    SHARED_OUT,     // Item is currently with someone for a FOR_SHARE listing
    UNAVAILABLE,    // Lister has made it temporarily or permanently unavailable
    DRAFT           // Listing created but not yet published
}