package org.powertac.orchestrator.weather;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WeatherFileProvider {
  private final Logger log = LogManager.getLogger(WeatherFileProvider.class);
  private final ResourceLoader resourceLoader;

  @Value("${weather.files.location:classpath:weather/}")
  private String weatherFilesLocation;

  public WeatherFileProvider(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  public Resource getWeatherFileForCity(String city) {
    final Logger logger = LogManager.getLogger(WeatherFileProvider.class);
    String filename = city.toLowerCase() + ".xml";
    String location = weatherFilesLocation + filename;
    logger.debug("Loading weather file from: {}", location);
    return resourceLoader.getResource(location);
  }

  public List<Resource> getAllWeatherFiles() {
    final Logger logger = LogManager.getLogger(WeatherFileProvider.class);
    try {
      Resource directory = resourceLoader.getResource(weatherFilesLocation);
      if (directory.exists()) {
        File dir = directory.getFile();
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".xml"));
        if (files != null) {
          return Arrays.stream(files)
                  .map(file -> resourceLoader.getResource(weatherFilesLocation + file.getName()))
                  .collect(Collectors.toList());
        }
      }
      logger.warn("Weather files directory does not exist or is empty: {}", weatherFilesLocation);
      return Collections.emptyList();
    } catch (IOException e) {
      logger.error("Failed to list weather files", e);
      return Collections.emptyList();
    }
  }

}
