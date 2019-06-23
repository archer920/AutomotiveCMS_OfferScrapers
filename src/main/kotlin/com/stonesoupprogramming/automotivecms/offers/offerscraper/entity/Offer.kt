package com.stonesoupprogramming.automotivecms.offers.offerscraper.entity

import java.util.*
import javax.persistence.*

@Entity
@Table(name="offer")
data class Offer (
        @Id
        @GeneratedValue
        var id: Long? = null,

        var createdDate: Date? = null,

        @Lob
        var disclaimer: String? = null,
        var image_url: String? = null,
        var inventory_link: String? = null,

        @Enumerated(EnumType.STRING)
        var offerType: OfferType? = null,
        var phoneNumber: String? = null,
        var price: String? = null,
        var source: String? = null,
        var title: String? = null,
        var vin: String? = null,
        var dealershipId: Long? = null)
