# Sluice

Sluice is a geographic boundary pipeline and spatial streaming service for pbu projects, including [Kaiju](https://github.com/pbu-projects/Kaiju).

## What Sluice Does

Every community project, volunteer event, and outreach team works within real physical boundaries, like states, counties, and cities.

When users sign up or look for projects near their homes, applications need to know:
1. Which state, county, or city contains a coordinate pin?
2. What are the official shapes and borders of these areas?
3. What is the local time zone for scheduling events?

Sluice answers these questions fast and reliably. It provides:
- Authoritative state boundaries for all 50 US states, Puerto Rico, and Washington DC.
- County and metro city boundary lookups.
- Point in polygon coordinate resolution (reverse geocoding) to find which civic borders contain a latitude and longitude.
- Offline support with pre-loaded simplified shapes, so services continue working without internet access or when government servers are down.

## Channeling Boundaries from US Census TIGERweb

The United States Census Bureau publishes official boundary maps through a system called [TIGERweb](https://tigerweb.geo.census.gov/).

Government maps are detailed down to the exact curvature of rivers, ditches, and country roads. While that detail is great for surveyors, raw files often contain hundreds of thousands of coordinate points. Sending huge raw files across mobile networks slows down mobile apps and drains phone batteries.

Sluice solves this problem by acting as a sluice channel:
- It connects to TIGERweb MapServer endpoints for states (layer 80), counties (layer 82), and incorporated places (layer 28).
- It simplifies boundary shapes using the [Douglas-Peucker algorithm](https://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm) while preserving true topology and enclaves.
- It caches and serves lightweight GeoJSON data over clean HTTP REST endpoints.

## How Companion Projects Like Kaiju Use Sluice

Companion projects like Kaiju call Sluice to manage spatial areas:

1. **Creating and Validating Administrative Regions**
   When an organizer creates a project in Denver, Kaiju asks Sluice for the Denver boundary. Kaiju saves this simplified shape in its database for fast spatial checks.

2. **Resolving Map Pins to Civic Jurisdictions**
   When a volunteer shares their current location (such as latitude 39.7392, longitude -104.9903), Kaiju calls `GET /api/v1/boundaries/resolve?lat=39.7392&lon=-104.9903`. Sluice immediately returns:
   - State: `CO`
   - County: `Denver County`
   - City: `Denver`
   - Time Zone: `America/Denver`

3. **Preventing Outages**
   Because Sluice bundles curated seed shapes right on the classpath, local testing, continuous integration, and disaster relief workflows run smoothly even if outside government APIs go offline.

## REST API Endpoints

All endpoints are hosted under `/api/v1/boundaries`:

| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/boundaries/states` | GET | Lists all US states and territories |
| `/api/v1/boundaries/states/{stateCode}` | GET | Gets the boundary and GeoJSON for a specific state (e.g. `CO` or `CA`) |
| `/api/v1/boundaries/counties?state={stateCode}` | GET | Lists counties for a given state |
| `/api/v1/boundaries/cities?state={stateCode}&county={countyName}` | GET | Lists cities filtered by state and optional county |
| `/api/v1/boundaries/resolve?lat={lat}&lon={lon}` | GET | Finds which state, county, and city contains a coordinate point |
| `/api/v1/boundaries/health` | GET | Health check returning service status |

## Building and Running Locally

Requirements:
- Java 25 or higher

Run tests:
```bash
./gradlew test
```

Start the application:
```bash
./gradlew run
```
