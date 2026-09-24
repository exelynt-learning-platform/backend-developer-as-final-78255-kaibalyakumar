package com.kaibalya.bookingsystem;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BookingSystemApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Value("${app.seed.user-password}")
    private String userPassword;

    private static String adminToken;
    private static String userToken;

    private static Long resourceId;
    private static Long adminReservationId;

    /*
     * ---------------------------------------------------------
     * 1. LOGIN TESTS
     * ---------------------------------------------------------
     */

    @Test
    @Order(1)
    void adminLoginShouldReturnJwtToken() throws Exception {

        String request = """
                {
                    "username": "admin",
                    "password": "%s"
                }
                """.formatted(adminPassword);

        String response = mockMvc.perform(
                post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.type").value("Bearer"))
        .andExpect(jsonPath("$.username").value("admin"))
        .andExpect(jsonPath("$.role").value("ADMIN"))
        .andReturn()
        .getResponse()
        .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        adminToken = json.get("token").asText();
    }

    @Test
    @Order(2)
    void userLoginShouldReturnJwtToken() throws Exception {

        String request = """
                {
                    "username": "user",
                    "password": "%s"
                }
                """.formatted(userPassword);

        String response = mockMvc.perform(
                post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.type").value("Bearer"))
        .andExpect(jsonPath("$.username").value("user"))
        .andExpect(jsonPath("$.role").value("USER"))
        .andReturn()
        .getResponse()
        .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        userToken = json.get("token").asText();
    }

    @Test
    @Order(3)
    void loginWithInvalidPasswordShouldFail() throws Exception {

        String request = """
                {
                    "username": "admin",
                    "password": "wrong-password"
                }
                """;

        mockMvc.perform(
                post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isUnauthorized());
    }

    /*
     * ---------------------------------------------------------
     * 2. AUTHENTICATION TESTS
     * ---------------------------------------------------------
     */

    @Test
    @Order(4)
    void protectedEndpointWithoutTokenShouldReturnUnauthorized() throws Exception {

        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    void authenticatedUserShouldAccessResources() throws Exception {

        mockMvc.perform(
                get("/api/resources")
                        .header("Authorization", "Bearer " + userToken)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
    }

    /*
     * ---------------------------------------------------------
     * 3. RESOURCE / RBAC TESTS
     * ---------------------------------------------------------
     */

    @Test
    @Order(6)
    void adminShouldBeAbleToCreateResource() throws Exception {

        String request = """
                {
                    "name": "Test Meeting Room",
                    "description": "Created by automated test",
                    "type": "ROOM",
                    "location": "Test Floor",
                    "capacity": 10,
                    "active": true
                }
                """;

        String response = mockMvc.perform(
                post("/api/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Test Meeting Room"))
        .andReturn()
        .getResponse()
        .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        resourceId = json.get("id").asLong();
    }

    @Test
    @Order(7)
    void userShouldNotBeAbleToCreateResource() throws Exception {

        String request = """
                {
                    "name": "Unauthorized Room",
                    "description": "Should not be created",
                    "type": "ROOM",
                    "location": "Test",
                    "capacity": 5,
                    "active": true
                }
                """;

        mockMvc.perform(
                post("/api/resources")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    void userShouldNotBeAbleToDeleteResource() throws Exception {

        mockMvc.perform(
                delete("/api/resources/" + resourceId)
                        .header("Authorization", "Bearer " + userToken)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @Order(9)
    void userShouldBeAbleToReadResources() throws Exception {

        mockMvc.perform(
                get("/api/resources")
                        .header("Authorization", "Bearer " + userToken)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
    }

    /*
     * ---------------------------------------------------------
     * 4. RESOURCE VALIDATION TEST
     * ---------------------------------------------------------
     */

    @Test
    @Order(10)
    void resourceWithInvalidCapacityShouldBeRejected() throws Exception {

        String request = """
                {
                    "name": "Invalid Room",
                    "description": "Invalid capacity test",
                    "type": "ROOM",
                    "location": "Test",
                    "capacity": 0,
                    "active": true
                }
                """;

        mockMvc.perform(
                post("/api/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    /*
     * ---------------------------------------------------------
     * 5. RESERVATION TESTS
     * ---------------------------------------------------------
     */

    @Test
    @Order(11)
    void userShouldBeAbleToCreateReservation() throws Exception {

        String start = LocalDateTime.now()
                .plusDays(10)
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toString();

        String end = LocalDateTime.now()
                .plusDays(10)
                .withHour(12)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toString();

        String request = """
                {
                    "resourceId": %d,
                    "startTime": "%s",
                    "endTime": "%s",
                    "price": 100.00
                }
                """.formatted(resourceId, start, end);

        mockMvc.perform(
                post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.resourceId").value(resourceId))
        .andExpect(jsonPath("$.price").value(100.00));
    }

    @Test
    @Order(12)
    void userShouldSeeReservations() throws Exception {

        mockMvc.perform(
                get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(13)
    void adminShouldSeeReservations() throws Exception {

        mockMvc.perform(
                get("/api/reservations")
                        .header("Authorization", "Bearer " + adminToken)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
    }

    /*
     * ---------------------------------------------------------
     * 6. RESERVATION VALIDATION
     * ---------------------------------------------------------
     */

    @Test
    @Order(14)
    void negativeReservationPriceShouldBeRejected() throws Exception {

        String start = LocalDateTime.now()
                .plusDays(20)
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toString();

        String end = LocalDateTime.now()
                .plusDays(20)
                .withHour(12)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toString();

        String request = """
                {
                    "resourceId": %d,
                    "startTime": "%s",
                    "endTime": "%s",
                    "price": -10.00
                }
                """.formatted(resourceId, start, end);

        mockMvc.perform(
                post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    /*
     * ---------------------------------------------------------
     * 7. RESERVATION FILTERING
     * ---------------------------------------------------------
     */

    @Test
    @Order(15)
    void reservationFilteringByPriceShouldWork() throws Exception {

        mockMvc.perform(
                get("/api/reservations")
                        .param("minPrice", "0")
                        .param("maxPrice", "1000")
                        .header("Authorization", "Bearer " + userToken)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
    }

    /*
     * ---------------------------------------------------------
     * 8. PAGINATION
     * ---------------------------------------------------------
     */

    @Test
    @Order(16)
    void reservationPaginationShouldWork() throws Exception {

        mockMvc.perform(
                get("/api/reservations")
                        .param("page", "0")
                        .param("size", "5")
                        .header("Authorization", "Bearer " + userToken)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.size").value(5));
    }

    /*
     * ---------------------------------------------------------
     * 9. ADMIN-ONLY RESERVATION UPDATE
     * ---------------------------------------------------------
     */

    @Test
    @Order(17)
    void userShouldNotUpdateReservation() throws Exception {

        /*
         * Find an existing reservation first.
         */
        String response = mockMvc.perform(
                get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
        )
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

        JsonNode content = objectMapper.readTree(response)
                .get("content");

        if (content != null && content.size() > 0) {

            long reservationId = content.get(0)
                    .get("id")
                    .asLong();

            String request = """
                    {
                        "resourceId": %d,
                        "startTime": "%s",
                        "endTime": "%s",
                        "price": 200.00
                    }
                    """.formatted(
                    resourceId,
                    LocalDateTime.now().plusDays(30).withHour(10),
                    LocalDateTime.now().plusDays(30).withHour(12)
            );

            mockMvc.perform(
                    put("/api/reservations/" + reservationId)
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request)
            )
            .andExpect(status().isForbidden());
        }
    }

    /*
     * ---------------------------------------------------------
     * 10. ADMIN RESERVATION STATUS UPDATE
     * ---------------------------------------------------------
     */

    @Test
    @Order(18)
    void adminShouldBeAbleToUpdateReservationStatus() throws Exception {

        String response = mockMvc.perform(
                get("/api/reservations")
                        .header("Authorization", "Bearer " + adminToken)
        )
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

        JsonNode content = objectMapper.readTree(response)
                .get("content");

        if (content != null && content.size() > 0) {

            long reservationId = content.get(0)
                    .get("id")
                    .asLong();

            String request = """
                    {
                        "status": "CONFIRMED"
                    }
                    """;

            mockMvc.perform(
                    patch("/api/reservations/" + reservationId + "/status")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request)
            )
            .andExpect(status().isOk());
        }
    }

    /*
     * ---------------------------------------------------------
     * 11. USER CANNOT UPDATE RESERVATION STATUS
     * ---------------------------------------------------------
     */

    @Test
    @Order(19)
    void userShouldNotUpdateReservationStatus() throws Exception {

        String response = mockMvc.perform(
                get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
        )
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

        JsonNode content = objectMapper.readTree(response)
                .get("content");

        if (content != null && content.size() > 0) {

            long reservationId = content.get(0)
                    .get("id")
                    .asLong();

            String request = """
                    {
                        "status": "CANCELLED"
                    }
                    """;

            mockMvc.perform(
                    patch("/api/reservations/" + reservationId + "/status")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request)
            )
            .andExpect(status().isForbidden());
        }
    }

    /*
     * ---------------------------------------------------------
     * 12. USER CAN CANCEL OWN RESERVATION
     * ---------------------------------------------------------
     */

    @Test
    @Order(20)
    void userShouldBeAbleToCancelOwnReservation() throws Exception {

        String start = LocalDateTime.now()
                .plusDays(40)
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toString();

        String end = LocalDateTime.now()
                .plusDays(40)
                .withHour(12)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toString();

        String createRequest = """
                {
                    "resourceId": %d,
                    "startTime": "%s",
                    "endTime": "%s",
                    "price": 150.00
                }
                """.formatted(resourceId, start, end);

        String response = mockMvc.perform(
                post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest)
        )
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

        long reservationId = objectMapper.readTree(response)
                .get("id")
                .asLong();

        mockMvc.perform(
                patch("/api/reservations/" + reservationId + "/cancel")
                        .header("Authorization", "Bearer " + userToken)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    /*
     * ---------------------------------------------------------
     * 13. INVALID RESERVATION REQUEST
     * ---------------------------------------------------------
     */

    @Test
    @Order(21)
    void reservationWithoutRequiredFieldsShouldBeRejected() throws Exception {

        String request = """
                {
                    "price": 50.00
                }
                """;

        mockMvc.perform(
                post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    /*
     * ---------------------------------------------------------
     * 14. INVALID RESOURCE REQUEST
     * ---------------------------------------------------------
     */

    @Test
    @Order(22)
    void resourceWithoutNameShouldBeRejected() throws Exception {

        String request = """
                {
                    "description": "Missing name",
                    "type": "ROOM",
                    "location": "Test",
                    "capacity": 10,
                    "active": true
                }
                """;

        mockMvc.perform(
                post("/api/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest());
    }
}