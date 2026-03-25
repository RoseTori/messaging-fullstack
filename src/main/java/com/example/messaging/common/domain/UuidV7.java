package com.example.messaging.common.domain;

import java.util.UUID;

public final class UuidV7 {

    private UuidV7() {
    }

    public static UUID randomUuid() {
        return UUID.randomUUID();
    }
}
