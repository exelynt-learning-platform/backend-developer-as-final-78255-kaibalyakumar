package com.kaibalya.bookingsystem.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;

    /*
     * Seeder is disabled by default.
     * Enable it explicitly in application.properties for local development.
     */
    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    /*
     * Seed credentials are supplied through environment variables.
     * Do not hardcode passwords in source code.
     */
    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Value("${app.seed.user-password}")
    private String userPassword;

    @Override
    public void run(String... args) {

        if (!seedEnabled) {
            return;
        }

        // Create demo ADMIN and USER accounts
        User admin = seedUser(
                "admin",
                adminPassword,
                "admin@bookingsystem.com",
                Role.ADMIN
        );

        User user = seedUser(
                "user",
                userPassword,
                "user@bookingsystem.com",
                Role.USER
        );

        /*
         * Create sample resources only when the database
         * does not already contain resources.
         */
        if (resourceRepository.count() == 0) {

            Resource conferenceRoom = resourceRepository.save(
                    Resource.builder()
                            .name("Conference Room A")
                            .description(
                                    "Large conference room with projector and video conferencing")
                            .type("ROOM")
                            .location("Floor 3, Building 1")
                            .capacity(12)
                            .active(true)
                            .build()
            );

            Resource sedan = resourceRepository.save(
                    Resource.builder()
                            .name("Toyota Camry")
                            .description("Company sedan for local business trips")
                            .type("VEHICLE")
                            .location("Parking Lot B")
                            .capacity(4)
                            .active(true)
                            .build()
            );

            resourceRepository.save(
                    Resource.builder()
                            .name("Projector Kit")
                            .description(
                                    "Portable projector with screen and HDMI cables")
                            .type("EQUIPMENT")
                            .location("Storage Room 2")
                            .capacity(1)
                            .active(true)
                            .build()
            );

            /*
             * Create sample reservations only when there are
             * no existing reservations.
             */
            if (reservationRepository.count() == 0) {

                LocalDateTime firstReservationStart =
                        LocalDateTime.now()
                                .plusDays(1)
                                .withHour(9)
                                .withMinute(0)
                                .withSecond(0)
                                .withNano(0);

                LocalDateTime firstReservationEnd =
                        LocalDateTime.now()
                                .plusDays(1)
                                .withHour(11)
                                .withMinute(0)
                                .withSecond(0)
                                .withNano(0);

                reservationRepository.save(
                        Reservation.builder()
                                .resource(conferenceRoom)
                                .user(user)
                                .startTime(firstReservationStart)
                                .endTime(firstReservationEnd)
                                .price(new BigDecimal("50.00"))
                                .status(ReservationStatus.PENDING)
                                .build()
                );

                LocalDateTime secondReservationStart =
                        LocalDateTime.now()
                                .plusDays(2)
                                .withHour(8)
                                .withMinute(0)
                                .withSecond(0)
                                .withNano(0);

                LocalDateTime secondReservationEnd =
                        LocalDateTime.now()
                                .plusDays(2)
                                .withHour(18)
                                .withMinute(0)
                                .withSecond(0)
                                .withNano(0);

                reservationRepository.save(
                        Reservation.builder()
                                .resource(sedan)
                                .user(user)
                                .startTime(secondReservationStart)
                                .endTime(secondReservationEnd)
                                .price(new BigDecimal("120.00"))
                                .status(ReservationStatus.CONFIRMED)
                                .build()
                );
            }
        }
    }

    /**
     * Creates a user only if the username does not already exist.
     * Passwords are always stored using BCrypt hashing.
     */
    private User seedUser(
            String username,
            String rawPassword,
            String email,
            Role role) {

        return userRepository.findByUsername(username)
                .orElseGet(() ->
                        userRepository.save(
                                User.builder()
                                        .username(username)
                                        .password(passwordEncoder.encode(rawPassword))
                                        .email(email)
                                        .role(role)
                                        .enabled(true)
                                        .build()
                        )
                );
    }
}