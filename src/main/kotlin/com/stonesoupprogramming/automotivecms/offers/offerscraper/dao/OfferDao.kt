package com.stonesoupprogramming.automotivecms.offers.offerscraper.dao

import com.stonesoupprogramming.automotivecms.offers.offerscraper.entity.Offer
import org.springframework.data.jpa.repository.JpaRepository

interface OfferDao: JpaRepository<Offer, Long>
