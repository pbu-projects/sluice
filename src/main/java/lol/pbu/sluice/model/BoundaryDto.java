package lol.pbu.sluice.model;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.util.Map;

@Serdeable
public record BoundaryDto(
        String id,
        String name,
        BoundaryLevel level,
        @Nullable String stateCode,
        @Nullable String countyName,
        @Nullable String geometryGeoJson,
        @Nullable Map<String, Object> properties
) {
}
