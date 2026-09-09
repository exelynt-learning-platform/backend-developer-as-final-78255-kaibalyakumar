package com.kaibalya.bookingsystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import com.kaibalya.bookingsystem.dto.request.ReservationRequest;
import com.kaibalya.bookingsystem.dto.request.ReservationStatusUpdateRequest;
import com.kaibalya.bookingsystem.dto.response.ReservationResponse;
import com.kaibalya.bookingsystem.entity.ReservationStatus;
import com.kaibalya.bookingsystem.security.CustomUserDetails;
import com.kaibalya.bookingsystem.service.ReservationService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Create and manage resource reservations")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(summary = "Create a reservation. Identity is always taken from the JWT, never the request body.")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request,
                                                        Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(request, username));
    }

    @GetMapping
    @Operation(summary = "List reservations. ADMIN sees all; USER sees only their own. Supports filtering, pagination and sorting.")
    public ResponseEntity<Page<ReservationResponse>> search(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "id") Pageable pageable,
            Authentication authentication) {

        CustomUserDetails principal = currentUser(authentication);
        boolean isAdmin = isAdmin(authentication);
        Long restrictToUserId = isAdmin ? null : principal.getId();

        return ResponseEntity.ok(reservationService.search(status, minPrice, maxPrice, restrictToUserId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a reservation by id. Owner or ADMIN only.")
    public ResponseEntity<ReservationResponse> getById(@PathVariable Long id, Authentication authentication) {
        CustomUserDetails principal = currentUser(authentication);
        boolean isAdmin = isAdmin(authentication);
        return ResponseEntity.ok(reservationService.findByIdForUser(id, principal.getId(), isAdmin));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fully update a reservation (ADMIN only)")
    public ResponseEntity<ReservationResponse> update(@PathVariable Long id, @Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update reservation status, e.g. CONFIRM or CANCEL (ADMIN only)")
    public ResponseEntity<ReservationResponse> updateStatus(@PathVariable Long id,
                                                              @Valid @RequestBody ReservationStatusUpdateRequest request) {
        return ResponseEntity.ok(reservationService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel own reservation (owner) or any reservation (ADMIN)")
    public ResponseEntity<ReservationResponse> cancel(@PathVariable Long id, Authentication authentication) {
        CustomUserDetails principal = currentUser(authentication);
        boolean isAdmin = isAdmin(authentication);
        return ResponseEntity.ok(reservationService.cancelOwn(id, principal.getId(), isAdmin));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a reservation (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private CustomUserDetails currentUser(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    private boolean isAdmin(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals("ROLE_ADMIN")) {
                return true;
            }
        }
        return false;
    }
}
