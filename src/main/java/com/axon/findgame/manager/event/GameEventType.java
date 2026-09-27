package com.axon.findgame.manager.event;

import java.util.function.Supplier;

public record GameEventType(
        String id,
        String displayName,
        String description,
        int weight,
        Supplier<GameEvent> factory
) {}