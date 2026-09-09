package com.kaibalya.bookingsystem.service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kaibalya.bookingsystem.dto.request.ReservationRequest;
import com.kaibalya.bookingsystem.dto.request.ReservationStatusUpdateRequest;
import com.kaibalya.bookingsystem.dto.response.ReservationResponse;
import com.kaibalya.bookingsystem.entity.Reservation;
import com.kaibalya.bookingsystem.entity.ReservationStatus;
import com.kaibalya.bookingsystem.entity.Resource;
import com.kaibalya.bookingsystem.entity.User;
import com.kaibalya.bookingsystem.exception.BadRequestException;
import com.kaibalya.bookingsystem.exception.ResourceNotFoundException;
import com.kaibalya.bookingsystem.repository.ReservationRepository;
import com.kaibalya.bookingsystem.repository.UserRepository;
import com.kaibalya.bookingsystem.specification.ReservationSpecification;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ResourceService resourceService;

    @Transactional
    public ReservationResponse create(ReservationRequest request, String username) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("endTime must be after startTime");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Resource resource = resourceService.getEntityOrThrow(request.getResourceId());

        if (!resource.isActive()) {
            throw new BadRequestException("Resource is not active and cannot be reserved");
        }

        Reservation reservation = Reservation.builder()
                .resource(resource)
                .user(user)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .price(request.getPrice())
                .status(ReservationStatus.PENDING) // identity & initial status always server-controlled
                .build();

        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> search(ReservationStatus status, BigDecimal minPrice, BigDecimal maxPrice,
                                             Long restrictToUserId, Pageable pageable) {
        var spec = ReservationSpecification.build(status, minPrice, maxPrice, restrictToUserId);
        return reservationRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReservationResponse findByIdForUser(Long id, Long currentUserId, boolean isAdmin) {
        Reservation reservation = getEntityOrThrow(id);
        enforceOwnership(reservation, currentUserId, isAdmin);
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse update(Long id, ReservationRequest request) {
        Reservation reservation = getEntityOrThrow(id);

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("endTime must be after startTime");
        }

        Resource resource = resourceService.getEntityOrThrow(request.getResourceId());

        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(request.getPrice());

        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse updateStatus(Long id, ReservationStatusUpdateRequest request) {
        Reservation reservation = getEntityOrThrow(id);
        reservation.setStatus(request.getStatus());
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse cancelOwn(Long id, Long currentUserId, boolean isAdmin) {
        Reservation reservation = getEntityOrThrow(id);
        enforceOwnership(reservation, currentUserId, isAdmin);
        reservation.setStatus(ReservationStatus.CANCELLED);
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public void delete(Long id) {
        Reservation reservation = getEntityOrThrow(id);
        reservationRepository.delete(reservation);
    }

    private void enforceOwnership(Reservation reservation, Long currentUserId, boolean isAdmin) {
        if (!isAdmin && !reservation.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have access to this reservation");
        }
    }

    private Reservation getEntityOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .resourceId(reservation.getResource().getId())
                .resourceName(reservation.getResource().getName())
                .userId(reservation.getUser().getId())
                .username(reservation.getUser().getUsername())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .price(reservation.getPrice())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}

