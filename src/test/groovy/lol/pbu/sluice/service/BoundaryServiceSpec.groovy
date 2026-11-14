package lol.pbu.sluice.service

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import lol.pbu.sluice.model.BoundaryDto
import lol.pbu.sluice.model.BoundaryLevel
import lol.pbu.sluice.model.ReverseGeocodeResult
import org.locationtech.jts.geom.Geometry
import spock.lang.Specification

@MicronautTest
class BoundaryServiceSpec extends Specification {

    @Inject
    BoundaryService boundaryService

    def "seed data loads all US states and territories"() {
        when:
        List<BoundaryDto> states = boundaryService.getStates()

        then:
        verifyAll {
            !states.isEmpty()
            states.size() >= 50
            states.any { it.stateCode() == "CO" }
            states.any { it.stateCode() == "CA" }
            states.any { it.stateCode() == "NY" }
            states.any { it.stateCode() == "TX" }
        }
    }

    def "getting a state by code or name returns expected boundary"() {
        when:
        Optional<BoundaryDto> colorado = boundaryService.getState("CO")
        Optional<BoundaryDto> lowercase = boundaryService.getState("co")
        Optional<BoundaryDto> byName = boundaryService.getState("Colorado")

        then:
        verifyAll {
            colorado.isPresent()
            colorado.get().stateCode() == "CO"
            colorado.get().name() == "Colorado"
            colorado.get().level() == BoundaryLevel.STATE
            colorado.get().geometryGeoJson() != null
            lowercase.isPresent()
            byName.isPresent()
        }
    }

    def "getting an unknown state returns empty"() {
        when:
        Optional<BoundaryDto> missing = boundaryService.getState("ZZ")

        then:
        verifyAll {
            missing.isEmpty()
        }
    }

    def "getting counties filters by state code"() {
        when:
        List<BoundaryDto> coloradoCounties = boundaryService.getCounties("CO")
        List<BoundaryDto> allCounties = boundaryService.getCounties(null)

        then:
        verifyAll {
            !coloradoCounties.isEmpty()
            coloradoCounties.every { it.stateCode() == "CO" }
            coloradoCounties.every { it.level() == BoundaryLevel.COUNTY }
            coloradoCounties.find { it.name() == "Denver County" } != null
            coloradoCounties.find { it.name() == "Arapahoe County" } != null
            allCounties.size() >= coloradoCounties.size()
        }
    }

    def "getting cities filters by state and county"() {
        when:
        List<BoundaryDto> denverCities = boundaryService.getCities("CO", "Denver County")
        List<BoundaryDto> coloradoCities = boundaryService.getCities("CO", null)

        then:
        verifyAll {
            !denverCities.isEmpty()
            denverCities.every { it.stateCode() == "CO" && it.countyName() == "Denver County" }
            denverCities.every { it.level() == BoundaryLevel.CITY }
            denverCities.find { it.name() == "Denver" } != null
            coloradoCities.find { it.name() == "Aurora" } != null
            coloradoCities.find { it.name() == "Lakewood" } != null
        }
    }

    def "resolving Denver coordinates returns state county city and timezone"() {
        given:
        double lat = 39.7392
        double lon = -104.9903

        when:
        ReverseGeocodeResult result = boundaryService.resolve(lat, lon)

        then:
        verifyAll(result) {
            state() == "CO"
            county() == "Denver County"
            city() == "Denver"
            timeZone() == "America/Denver"
        }
    }

    def "resolving Chicago coordinates returns Illinois and Cook County"() {
        given:
        double lat = 41.8781
        double lon = -87.6298

        when:
        ReverseGeocodeResult result = boundaryService.resolve(lat, lon)

        then:
        verifyAll(result) {
            state() == "IL"
            county() == "Cook County"
            city() == "Chicago"
            timeZone() == "America/Chicago"
        }
    }

    def "resolving point outside United States returns null components"() {
        given:
        double lat = 0.0
        double lon = 0.0

        when:
        ReverseGeocodeResult result = boundaryService.resolve(lat, lon)

        then:
        verifyAll(result) {
            state() == null
            county() == null
            city() == null
            timeZone() == null
        }
    }

    def "loading uncompressed boundary json falls back when gz archive is absent"() {
        given:
        Map<String, Geometry> geometries = [:]

        when:
        List<BoundaryDto> loaded = boundaryService.loadBoundaries("/boundaries/plain-test.json", geometries)

        then:
        verifyAll {
            loaded.size() == 1
            loaded[0].id() == "TEST-1"
            geometries.containsKey("TEST-1")
        }
    }

    def "loading missing or corrupt boundary resources returns empty list"() {
        given:
        Map<String, Geometry> missingGeometries = [:]
        Map<String, Geometry> corruptGeometries = [:]

        when:
        List<BoundaryDto> missing = boundaryService.loadBoundaries("/boundaries/does-not-exist.json", missingGeometries)
        List<BoundaryDto> corrupt = boundaryService.loadBoundaries("/boundaries/corrupt-test.json", corruptGeometries)

        then:
        verifyAll {
            missing.isEmpty()
            missingGeometries.isEmpty()
            corrupt.isEmpty()
            corruptGeometries.isEmpty()
        }
    }

    def "extracting timezone ignores blank or missing values"() {
        given:
        BoundaryDto blankTz = new BoundaryDto("B-1", "Blank", BoundaryLevel.CITY, "CO", null, null, [timeZone: "   "])
        BoundaryDto noProps = new BoundaryDto("B-2", "NoProps", BoundaryLevel.CITY, "CO", null, null, null)
        BoundaryDto validTz = new BoundaryDto("B-3", "Valid", BoundaryLevel.STATE, "CO", null, null, [timeZone: "America/Denver"])

        when:
        Optional<String> fromBlank = boundaryService.extractTimeZone(Optional.of(blankTz))
        Optional<String> fromNoProps = boundaryService.extractTimeZone(Optional.of(noProps))
        Optional<String> fromValid = boundaryService.extractTimeZone(Optional.of(validTz))

        then:
        verifyAll {
            fromBlank.isEmpty()
            fromNoProps.isEmpty()
            fromValid == Optional.of("America/Denver")
        }
    }
}
