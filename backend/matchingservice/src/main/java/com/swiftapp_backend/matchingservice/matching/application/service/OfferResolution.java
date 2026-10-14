package com.swiftapp_backend.matchingservice.matching.application.service;

/**
 * Outcome of an accept / reject / expire attempt on an offer.
 */
public enum OfferResolution {

    /** Offer was created (create path only). */
    CREATED,

    /** Driver accepted; ride is matched. */
    ACCEPTED,

    /**
     * Driver rejected; caller should reassign only when every targeted
     * driver has declined (see {@link #DECLINED}).
     */
    REJECTED,

    /** One driver declined but others are still pending; keep waiting. */
    DECLINED,

    /** Offer timed out; caller should reassign. */
    EXPIRED,

    /** No offer exists for this ride. */
    NO_OFFER,

    /** Offer is for a different driver than the one responding. */
    WRONG_DRIVER,

    /** Offer already left PENDING (accept raced timeout, double accept...). */
    ALREADY_SETTLED
}
