package com.kaibalya.bookingsystem.specification;

import org.springframework.data.jpa.domain.Specification;

import com.kaibalya.bookingsystem.entity.Reservation;
import com.kaibalya.bookingsystem.entity.ReservationStatus;

import java.math.BigDecimal;

public final class ReservationSpecification {

    private ReservationSpecification() {
    }

    public static Specification<Reservation> hasStatus(ReservationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Reservation> hasMinPrice(BigDecimal minPrice) {
        return (root, query, cb) -> minPrice == null ? null : cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Reservation> hasMaxPrice(BigDecimal maxPrice) {
        return (root, query, cb) -> maxPrice == null ? null : cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Reservation> belongsToUser(Long userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Reservation> build(ReservationStatus status, BigDecimal minPrice,
                                                     BigDecimal maxPrice, Long userId) {
        return Specification.where(hasStatus(status))
                .and(hasMinPrice(minPrice))
                .and(hasMaxPrice(maxPrice))
                .and(belongsToUser(userId));
    }
}
