package com.ticketmesh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * A single inventory item (ticket / service) uploaded by an agent shop. Any
 * shop can upload its ticket details and services; customers can buy them
 * directly or the shop sells them on behalf of customers.
 */
@Entity
@Table(name = "provider_products")
public class ProviderProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductType productType;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @Size(max = 120)
    @Column(length = 120)
    private String origin;

    @Size(max = 120)
    @Column(length = 120)
    private String destination;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(precision = 8, scale = 4)
    private BigDecimal taxRate;

    @Column(precision = 12, scale = 2)
    private BigDecimal serviceFee;

    @NotNull
    @Size(min = 3, max = 3)
    @Column(nullable = false, length = 3)
    private String currencyIso;

    @PositiveOrZero
    @Column(nullable = false)
    private int availableQuantity;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    @Column(columnDefinition = "LONGTEXT")
    private String attributes;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    // ── Offer / deal flags ──
    // When is_offer = true the product is a time-limited special deal and
    // appears in the Offers section of the home page. Original price + deal
    // pricing are stored inline to avoid a second table.
    @Column(name = "is_offer", nullable = false)
    private boolean isOffer = false;

    @Column(name = "original_price", precision = 12, scale = 2)
    private java.math.BigDecimal originalPrice;

    @Column(name = "discount_percent")
    private Integer discountPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "deal_type", length = 20)
    private DealType dealType;

    @Size(max = 40)
    @Column(name = "deal_tag", length = 40)
    private String dealTag;

    @Column(name = "deal_valid_until")
    private Instant dealValidUntil;

    @Column(name = "deal_seats")
    private Integer dealSeats;

    @Size(max = 500)
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    // ── Movie / event metadata ──
    @Size(max = 40)
    @Column(length = 40)
    private String language;

    @Size(max = 120)
    @Column(length = 120)
    private String genre;

    @Size(max = 40)
    @Column(length = 40)
    private String format;       // 2D, 3D, IMAX, 4DX, ATMOS

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "rating_stars", precision = 2, scale = 1)
    private java.math.BigDecimal ratingStars;

    @Size(max = 500)
    @Column(name = "cast_list", length = 500)
    private String castList;

    @Size(max = 160)
    @Column(length = 160)
    private String director;

    @Column(name = "release_date")
    private java.time.LocalDate releaseDate;

    @Size(max = 500)
    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Size(max = 500)
    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Column(name = "is_premiere", nullable = false)
    private boolean isPremiere = false;

    @Column(name = "is_now_showing", nullable = false)
    private boolean isNowShowing = true;

    @Size(max = 200)
    @Column(length = 200)
    private String tagline;

    public ProviderProduct() {
    }

    public ProviderProduct(Provider provider, Tenant tenant, ProductType productType,
                           String title, String origin, String destination,
                           LocalDateTime eventDate, BigDecimal price, String currencyIso,
                           int availableQuantity, String description, String attributes) {
        this.provider = provider;
        this.tenant = tenant;
        this.productType = productType;
        this.title = title;
        this.origin = origin;
        this.destination = destination;
        this.eventDate = eventDate;
        this.price = price;
        this.currencyIso = currencyIso;
        this.availableQuantity = availableQuantity;
        this.description = description;
        this.attributes = attributes;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Provider getProvider() {
        return provider;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public ProductType getProductType() {
        return productType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getServiceFee() {
        return serviceFee;
    }

    public void setServiceFee(BigDecimal serviceFee) {
        this.serviceFee = serviceFee;
    }

    public String getCurrencyIso() {
        return currencyIso;
    }

    public void setCurrencyIso(String currencyIso) {
        this.currencyIso = currencyIso;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public enum ProductType {
        TICKET,
        SERVICE,
        SEAT,
        ROUTE,
        ADMISSION,
        PACKAGE
    }

    public enum DealType {
        FLAT_OFF,
        PERCENTAGE_OFF,
        BUY_X_GET_Y,
        FLASH_SALE
    }

    // ── Offer/deal getters and setters ──

    public boolean isOffer() { return isOffer; }
    public void setOffer(boolean offer) { isOffer = offer; }

    public java.math.BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(java.math.BigDecimal originalPrice) { this.originalPrice = originalPrice; }

    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }

    public DealType getDealType() { return dealType; }
    public void setDealType(DealType dealType) { this.dealType = dealType; }

    public String getDealTag() { return dealTag; }
    public void setDealTag(String dealTag) { this.dealTag = dealTag; }

    public Instant getDealValidUntil() { return dealValidUntil; }
    public void setDealValidUntil(Instant dealValidUntil) { this.dealValidUntil = dealValidUntil; }

    public Integer getDealSeats() { return dealSeats; }
    public void setDealSeats(Integer dealSeats) { this.dealSeats = dealSeats; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public java.math.BigDecimal getRatingStars() { return ratingStars; }
    public void setRatingStars(java.math.BigDecimal ratingStars) { this.ratingStars = ratingStars; }

    public String getCastList() { return castList; }
    public void setCastList(String castList) { this.castList = castList; }

    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }

    public java.time.LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(java.time.LocalDate releaseDate) { this.releaseDate = releaseDate; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    public boolean isPremiere() { return isPremiere; }
    public void setPremiere(boolean premiere) { isPremiere = premiere; }

    public boolean isNowShowing() { return isNowShowing; }
    public void setNowShowing(boolean nowShowing) { isNowShowing = nowShowing; }

    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
}
