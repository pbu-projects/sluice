package lol.pbu.sluice.controller

import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import lol.pbu.sluice.model.BoundaryDto
import lol.pbu.sluice.model.BoundaryLevel
import lol.pbu.sluice.model.ReverseGeocodeResult
import spock.lang.Specification

@MicronautTest
class BoundaryControllerSpec extends Specification {

    @Inject
    @Client("/")
    HttpClient client

    def "health check returns up status"() {
        when:
        HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET("/api/v1/boundaries/health"),
                Map
        )

        then:
        verifyAll(response) {
            status == HttpStatus.OK
            body().status == "UP"
            body().service == "sluice"
        }
    }

    def "getting states returns all pre-loaded states"() {
        when:
        HttpResponse<List<BoundaryDto>> response = client.toBlocking().exchange(
                HttpRequest.GET("/api/v1/boundaries/states"),
                Argument.listOf(BoundaryDto)
        )

        then:
        verifyAll(response) {
            status == HttpStatus.OK
            body().size() >= 50
            body().any { it.stateCode() == "CO" && it.level() == BoundaryLevel.STATE }
        }
    }

    def "getting a specific state by code returns boundary GeoJSON"() {
        when:
        HttpResponse<BoundaryDto> response = client.toBlocking().exchange(
                HttpRequest.GET("/api/v1/boundaries/states/CO"),
                BoundaryDto
        )

        then:
        verifyAll(response) {
            status == HttpStatus.OK
            body().stateCode() == "CO"
            body().name() == "Colorado"
            body().geometryGeoJson() != null
        }
    }

    def "getting an unknown state returns 404 not found"() {
        when:
        client.toBlocking().exchange(
                HttpRequest.GET("/api/v1/boundaries/states/UNKNOWN"),
                BoundaryDto
        )

        then:
        HttpClientResponseException ex = thrown(HttpClientResponseException)
        ex.status == HttpStatus.NOT_FOUND
    }

    def "getting counties filtered by state"() {
        when:
        HttpResponse<List<BoundaryDto>> response = client.toBlocking().exchange(
                HttpRequest.GET("/api/v1/boundaries/counties?state=CO"),
                Argument.listOf(BoundaryDto)
        )

        then:
        verifyAll(response) {
            status == HttpStatus.OK
            !body().isEmpty()
            body().every { it.stateCode() == "CO" }
            body().any { it.name() == "Denver County" }
        }
    }

    def "getting cities filtered by state and county"() {
        when:
        HttpResponse<List<BoundaryDto>> response = client.toBlocking().exchange(
                HttpRequest.GET("/api/v1/boundaries/cities?state=CO&county=Denver%20County"),
                Argument.listOf(BoundaryDto)
        )

        then:
        verifyAll(response) {
            status == HttpStatus.OK
            !body().isEmpty()
            body().every { it.stateCode() == "CO" && it.countyName() == "Denver County" }
            body().any { it.name() == "Denver" }
        }
    }

    def "resolving coordinates returns reverse geocoding result"() {
        when:
        HttpResponse<ReverseGeocodeResult> response = client.toBlocking().exchange(
                HttpRequest.GET("/api/v1/boundaries/resolve?lat=39.7392&lon=-104.9903"),
                ReverseGeocodeResult
        )

        then:
        verifyAll(response) {
            status == HttpStatus.OK
            body().state() == "CO"
            body().county() == "Denver County"
            body().city() == "Denver"
            body().timeZone() == "America/Denver"
        }
    }
}
