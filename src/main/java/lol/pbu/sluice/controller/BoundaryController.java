package lol.pbu.sluice.controller;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import lol.pbu.sluice.model.BoundaryDto;
import lol.pbu.sluice.model.ReverseGeocodeResult;
import lol.pbu.sluice.service.BoundaryService;

import java.util.List;
import java.util.Map;

@Controller("/api/v1/boundaries")
public class BoundaryController {

    private final BoundaryService boundaryService;

    public BoundaryController(BoundaryService boundaryService) {
        this.boundaryService = boundaryService;
    }

    @Get("/states")
    public List<BoundaryDto> getStates() {
        return boundaryService.getStates();
    }

    @Get("/states/{stateCode}")
    public HttpResponse<BoundaryDto> getState(@PathVariable String stateCode) {
        return boundaryService.getState(stateCode)
                .map(HttpResponse::ok)
                .orElseGet(HttpResponse::notFound);
    }

    @Get("/counties")
    public List<BoundaryDto> getCounties(@QueryValue @Nullable String state) {
        return boundaryService.getCounties(state);
    }

    @Get("/cities")
    public List<BoundaryDto> getCities(
            @QueryValue @Nullable String state,
            @QueryValue @Nullable String county
    ) {
        return boundaryService.getCities(state, county);
    }

    @Get("/resolve")
    public ReverseGeocodeResult resolve(
            @QueryValue double lat,
            @QueryValue double lon
    ) {
        return boundaryService.resolve(lat, lon);
    }

    @Get("/health")
    public Map<String, String> getHealth() {
        return Map.of(
                "status", "UP",
                "service", "sluice"
        );
    }
}
