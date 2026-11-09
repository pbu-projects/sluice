package lol.pbu.sluice.model;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public enum BoundaryLevel {
    STATE,
    COUNTY,
    CITY
}
