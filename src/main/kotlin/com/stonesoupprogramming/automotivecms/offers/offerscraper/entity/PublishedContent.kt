package com.stonesoupprogramming.automotivecms.offers.offerscraper.entity

import java.util.*
import javax.persistence.*

@Entity
@Table(name = "abstract_content")
data class PublishedContent(
        @Id @GeneratedValue
        var id: Long? = null,

        var byline: String? = null,
        var date: Date? = null,
        var dismiss: Boolean? = null,
        var displayName: String? = null,
        var image: String? = null,

        @Lob
        var link: String? = null,

        @Lob
        var logo: String? = null,
        var publication: String? = null,

        @Lob
        var summary: String? = null,
        var title: String? = null,
        var rating: Double? = null,
        var dtype: String? = null
)
