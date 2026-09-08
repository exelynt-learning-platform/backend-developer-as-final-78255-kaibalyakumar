package com.kaibalya.bookingsystem.config;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.kaibalya.bookingsystem.entity.Reservation;
import com.kaibalya.bookingsystem.entity.ReservationStatus;
import com.kaibalya.bookingsystem.entity.Resource;
import com.kaibalya.bookingsystem.entity.Role;
import com.kaibalya.bookingsystem.entity.User;
import com.kaibalya.bookingsystem.repository.ReservationRepository;
import com.kaibalya.bookingsystem.repository.ResourceRepository;
import com.kaibalya.bookingsystem.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Seeds the database with demo ADMIN/USER accounts and a couple of sample
 * resources/reservations so the API is immediately testable.
 * Controlled by app.seed.enabled (default true). Safe to run repeatedly - it
 * only inserts data that does not already exist.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        User admin = seedUser("admin", "admin123", "admin@bookingsystem.com", Role.ADMIN);
        User user = seedUser("user", "user123", "user@bookingsystem.com", Role.USER);

        if (resourceRepository.count() == 0) {
            Resource conferenceRoom = resourceRepository.save(Resource.builder()
                    .name("Conference Room A")
                    .description("Large conference room with projector and video conferencing")
                    .type("ROOM")
                    .location("Floor 3, Building 1")
                    .capacity(12)
                    .active(true)
                    .build());

            Resource sedan = resourceRepository.save(Resource.builder()
                    .name("Toyota Camry")
                    .description("Company sedan for local business trips")
                    .type("VEHICLE")
                    .location("Parking Lot B")
                    .capacity(4)
                    .active(true)
                    .build());

            resourceRepository.save(Resource.builder()
                    .name("Projector Kit")
                    .description("Portable projector with screen and HDMI cables")
                    .type("EQUIPMENT")
                    .location("Storage Room 2")
                    .capacity(1)
                    .active(true)
                    .build());

            if (reservationRepository.count() == 0) {
                reservationRepository.save(Reservation.builder()
                        .resource(conferenceRoom)
                        .user(user)
                        .startTime(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0))
                        .endTime(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0))
                        .price(new BigDecimal("50.00"))
                        .status(ReservationStatus.PENDING)
                        .build());

                reservationRepository.save(Reservation.builder()
                        .resource(sedan)
                        .user(user)
                        .startTime(LocalDateTime.now().plusDays(2).withHour(8).withMinute(0))
                        .endTime(LocalDateTime.now().plusDays(2).withHour(18).withMinute(0))
                        .price(new BigDecimal("120.00"))
                        .status(ReservationStatus.CONFIRMED)
                        .build());
            }
        }
    }

    private User seedUser(String username, String rawPassword, String email, Role role) {
        return userRepository.findByUsername(username).orElseGet(() ->
                userRepository.save(User.builder()
                        .username(username)
                        .password(passwordEncoder.encode(rawPassword))
                        .email(email)
                        .role(role)
                        .enabled(true)
                        .build()));
    }
}

