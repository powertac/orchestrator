package org.powertac.orchestrator.weather;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WeatherFileProviderTest {

  private ResourceLoader resourceLoader;
  private WeatherFileProvider weatherFileProvider;

  @BeforeEach
  void setUp() {
    resourceLoader = mock(ResourceLoader.class);
    weatherFileProvider = new WeatherFileProvider(resourceLoader);
    ReflectionTestUtils.setField(weatherFileProvider, "weatherFilesLocation", "classpath:weather/");
  }

  @Test
  void getWeatherFileForCity_returnsResource() {
    String city = "berlin";
    Resource resource = mock(Resource.class);
    when(resourceLoader.getResource("classpath:weather/berlin.xml")).thenReturn(resource);

    Resource result = weatherFileProvider.getWeatherFileForCity(city);

    assertNotNull(result);
    assertEquals(resource, result);
  }

  @Test
  void getWeatherFileForCity_cityNotFound_returnsNull() {
    String city = "unknown";
    when(resourceLoader.getResource("classpath:weather/unknown.xml")).thenReturn(null);

    Resource result = weatherFileProvider.getWeatherFileForCity(city);

    assertNull(result);
  }

  @Test
  void getAllWeatherFiles_returnsListOfResources() throws IOException {
    // Create a temporary directory
    Path tempDir = Files.createTempDirectory("weather-test");

    try {
      // Create test files
      Files.createFile(tempDir.resolve("berlin.xml"));
      Files.createFile(tempDir.resolve("paris.xml"));

      // Configure the provider to use this directory instead of classpath
      String fileUrl = "file:" + tempDir.toAbsolutePath() + "/";
      ReflectionTestUtils.setField(weatherFileProvider, "weatherFilesLocation", fileUrl);

      // Set up the resourceLoader to return real resources
      Resource dirResource = new FileSystemResource(tempDir.toFile());
      when(resourceLoader.getResource(fileUrl)).thenReturn(dirResource);
      when(resourceLoader.getResource(fileUrl + "berlin.xml"))
              .thenReturn(new FileSystemResource(tempDir.resolve("berlin.xml").toFile()));
      when(resourceLoader.getResource(fileUrl + "paris.xml"))
              .thenReturn(new FileSystemResource(tempDir.resolve("paris.xml").toFile()));

      // Test the method
      List<Resource> result = weatherFileProvider.getAllWeatherFiles();

      // Assertions
      assertNotNull(result);
      assertEquals(2, result.size());

    } finally {
      // Clean up
      Files.walk(tempDir)
              .sorted(Comparator.reverseOrder())
              .forEach(path -> {
                try {
                  Files.delete(path);
                } catch (IOException e) {
                  e.printStackTrace();
                }
              });
    }
  }

  @Test
  void getAllWeatherFiles_directoryNotFound_returnsEmptyList()  {
    Resource directory = mock(Resource.class);
    when(resourceLoader.getResource("classpath:weather/")).thenReturn(directory);
    when(directory.exists()).thenReturn(false);

    List<Resource> result = weatherFileProvider.getAllWeatherFiles();

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

}
