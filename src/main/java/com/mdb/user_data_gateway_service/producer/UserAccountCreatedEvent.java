package com.mdb.user_data_gateway_service.producer;

import java.time.Instant;
import java.util.UUID;

public record UserAccountCreatedEvent(
    UUID userId,
    UUID accountId,
    String username,
    String email,
    Instant createdAt
) {}
