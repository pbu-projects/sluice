package lol.pbu.sluice.model;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record ReverseGeocodeResult(
        @Nullable String state,
        @Nullable String county,
        @Nullable String city,
        @Nullable String timeZone
) {
}
