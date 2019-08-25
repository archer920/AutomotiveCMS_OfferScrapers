package com.stonesoupprogramming.automotivecms.offers.offerscraper.dao

import com.stonesoupprogramming.automotivecms.offers.offerscraper.entity.Offer
import com.stonesoupprogramming.automotivecms.offers.offerscraper.entity.OfferType
import org.springframework.data.jpa.repository.JpaRepository
import javax.transaction.Transactional

interface OfferDao: JpaRepository<Offer, Long> {

    @Transactional
    fun deleteAllBySourceAndOfferType(source: String, offerType: OfferType)
}
