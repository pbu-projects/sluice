package lol.pbu.sluice.service;

import io.micronaut.context.annotation.Context;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.type.Argument;
import io.micronaut.serde.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import lol.pbu.sluice.model.BoundaryDto;
import lol.pbu.sluice.model.ReverseGeocodeResult;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Context
public class BoundaryService {

    private static final Logger log = LoggerFactory.getLogger(BoundaryService.class);

    private final ObjectMapper objectMapper;
    private final GeometryFactory geometryFactory;
    private final GeoJsonReader geoJsonReader;

    private List<BoundaryDto> states = Collections.emptyList();
    private List<BoundaryDto> counties = Collections.emptyList();
    private List<BoundaryDto> cities = Collections.emptyList();

    private final Map<String, Geometry> stateGeometries = new ConcurrentHashMap<>();
    private final Map<String, Geometry> countyGeometries = new ConcurrentHashMap<>();
    private final Map<String, Geometry> cityGeometries = new ConcurrentHashMap<>();

    public BoundaryService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        this.geoJsonReader = new GeoJsonReader(this.geometryFactory);
    }

    @PostConstruct
    public void loadSeedData() {
        this.states = loadBoundaries("/boundaries/states.json", stateGeometries);
        this.counties = loadBoundaries("/boundaries/counties.json", countyGeometries);
        this.cities = loadBoundaries("/boundaries/cities.json", cityGeometries);
    }

    private List<BoundaryDto> loadBoundaries(String resourcePath, Map<String, Geometry> geometryMap) {
        try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                log.warn("Boundary seed file not found: {}", resourcePath);
                return Collections.emptyList();
            }
            List<BoundaryDto> loaded = objectMapper.readValue(stream, Argument.listOf(BoundaryDto.class));
            loaded.forEach(dto -> {
                if (dto.geometryGeoJson() != null && !dto.geometryGeoJson().isBlank()) {
                    try {
                        Geometry geom = geoJsonReader.read(dto.geometryGeoJson());
                        geometryMap.put(dto.id(), geom);
                    } catch (ParseException e) {
                        log.warn("Failed to parse GeoJSON for boundary id {}: {}", dto.id(), e.getMessage());
                    }
                }
            });
            return loaded;
        } catch (IOException e) {
            log.error("Failed to read boundary seed data from {}: {}", resourcePath, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<BoundaryDto> getStates() {
        return states;
    }

    public Optional<BoundaryDto> getState(String stateCodeOrName) {
        if (stateCodeOrName == null || stateCodeOrName.isBlank()) {
            return Optional.empty();
        }
        String query = stateCodeOrName.trim();
        return states.stream()
                .filter(s -> (s.stateCode() != null && s.stateCode().equalsIgnoreCase(query))
                        || s.name().equalsIgnoreCase(query)
                        || s.id().equalsIgnoreCase(query)
                        || s.id().equalsIgnoreCase("US-" + query))
                .findFirst();
    }

    public List<BoundaryDto> getCounties(@Nullable String stateCode) {
        if (stateCode == null || stateCode.isBlank()) {
            return counties;
        }
        String cleanState = stateCode.trim();
        return counties.stream()
                .filter(c -> c.stateCode() != null && c.stateCode().equalsIgnoreCase(cleanState))
                .toList();
    }

    public List<BoundaryDto> getCities(@Nullable String stateCode, @Nullable String countyName) {
        return cities.stream()
                .filter(city -> stateCode == null || stateCode.isBlank()
                        || (city.stateCode() != null && city.stateCode().equalsIgnoreCase(stateCode.trim())))
                .filter(city -> countyName == null || countyName.isBlank()
                        || (city.countyName() != null && city.countyName().equalsIgnoreCase(countyName.trim())))
                .toList();
    }

    public ReverseGeocodeResult resolve(double lat, double lon) {
        Point point = geometryFactory.createPoint(new Coordinate(lon, lat));

        Optional<BoundaryDto> matchedState = states.stream()
                .filter(s -> covers(stateGeometries.get(s.id()), point))
                .findFirst();

        String stateCode = matchedState.map(BoundaryDto::stateCode).orElse(null);
        String stateName = matchedState.map(BoundaryDto::name).orElse(null);
        String timeZone = matchedState
                .map(s -> s.properties() != null ? (String) s.properties().get("timeZone") : null)
                .orElse(null);

        Optional<BoundaryDto> matchedCounty = counties.stream()
                .filter(c -> stateCode == null || (c.stateCode() != null && c.stateCode().equalsIgnoreCase(stateCode)))
                .filter(c -> covers(countyGeometries.get(c.id()), point))
                .findFirst();

        String countyName = matchedCounty.map(BoundaryDto::name).orElse(null);

        Optional<BoundaryDto> matchedCity = cities.stream()
                .filter(city -> stateCode == null || (city.stateCode() != null && city.stateCode().equalsIgnoreCase(stateCode)))
                .filter(city -> covers(cityGeometries.get(city.id()), point))
                .findFirst();

        String cityName = matchedCity.map(BoundaryDto::name).orElse(null);

        return new ReverseGeocodeResult(
                stateCode != null ? stateCode : stateName,
                countyName,
                cityName,
                timeZone
        );
    }

    private boolean covers(@Nullable Geometry geometry, Point point) {
        if (geometry == null) {
            return false;
        }
        if (!geometry.getEnvelopeInternal().covers(point.getCoordinate())) {
            return false;
        }
        return geometry.covers(point);
    }
}
