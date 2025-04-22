package org.powertac.orchestrator.weather;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class LocalWeatherLocationRepository implements WeatherLocationRepository {

  private final Logger log = LogManager.getLogger(LocalWeatherLocationRepository.class);
  private final WeatherFileProvider weatherFileProvider;

  public LocalWeatherLocationRepository(WeatherFileProvider weatherFileProvider) {
    this.weatherFileProvider = weatherFileProvider;
  }

  @Override
  @Cacheable("weatherLocations")
  public Set<WeatherLocation> findAllLocations() {
    List<Resource> weatherFiles = weatherFileProvider.getAllWeatherFiles();
    Set<WeatherLocation> locations = new HashSet<>();

    for (Resource weatherFile : weatherFiles) {
        locations.add(new WeatherLocation(weatherFile));
    }

    log.info("Found {} weather locations from files: {}",
             locations.size(),
             locations.stream().map(WeatherLocation::getName).collect(Collectors.joining(", ")));

    return locations;
  }
}
