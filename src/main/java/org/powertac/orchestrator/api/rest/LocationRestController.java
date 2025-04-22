package org.powertac.orchestrator.api.rest;

import org.powertac.orchestrator.weather.LocalWeatherLocationRepository;
import org.powertac.orchestrator.weather.WeatherLocation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/locations")
public class LocationRestController {

  private final LocalWeatherLocationRepository locationRepository;

  public LocationRestController(LocalWeatherLocationRepository locationRepository) {
    this.locationRepository = locationRepository;
  }

  @GetMapping("/")
  public ResponseEntity<Collection<WeatherLocation>> getLocations() {
    return ResponseEntity.ok(locationRepository.findAllLocations());
  }


}
