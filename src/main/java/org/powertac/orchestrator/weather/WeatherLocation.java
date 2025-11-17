package org.powertac.orchestrator.weather;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.io.Resource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import com.fasterxml.jackson.annotation.JsonFormat;

@Getter
@AllArgsConstructor
public class WeatherLocation {

    private String name;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant minReportTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant maxReportTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant minForecastTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant maxForecastTime;

        public WeatherLocation(Resource weatherFile) {
          try {
            String filename = weatherFile.getFilename();
            this.name = filename != null ? filename.replaceAll("\\.xml$", "") : "unknown";

            // Parse the XML file
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(weatherFile.getInputStream());
            doc.getDocumentElement().normalize();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            // Process weather reports using streams
            NodeList weatherReports = doc.getElementsByTagName("weatherReport");
            List<Instant> reportTimes = nodeListToStream(weatherReports)
                    .map(element -> element.getAttribute("date"))
                    .map(dateStr -> LocalDateTime.parse(dateStr, formatter)
                            .atZone(ZoneId.systemDefault())
                            .toInstant())
                    .toList();

            // Process weather forecasts using streams
            NodeList weatherForecasts = doc.getElementsByTagName("weatherForecast");
            List<Instant> forecastTimes = nodeListToStream(weatherForecasts)
                    .map(element -> element.getAttribute("date"))
                    .map(dateStr -> LocalDateTime.parse(dateStr, formatter)
                            .atZone(ZoneId.systemDefault())
                            .toInstant())
                    .toList();

            // Find min and max times
            this.minReportTime = reportTimes.isEmpty() ? Instant.MIN :
                    reportTimes.stream().min(Instant::compareTo).orElse(Instant.MIN);
            this.maxReportTime = reportTimes.isEmpty() ? Instant.MAX :
                    reportTimes.stream().max(Instant::compareTo).orElse(Instant.MAX);
            this.minForecastTime = forecastTimes.isEmpty() ? Instant.MIN :
                    forecastTimes.stream().min(Instant::compareTo).orElse(Instant.MIN);
            this.maxForecastTime = forecastTimes.isEmpty() ? Instant.MAX :
                    forecastTimes.stream().max(Instant::compareTo).orElse(Instant.MAX);
          } catch (Exception e) {
                throw new RuntimeException("Error initializing WeatherLocation from file", e);
          }

        }

  // Helper method to convert NodeList to Stream of Elements
  private Stream<Element> nodeListToStream(NodeList nodeList) {
    return IntStream.range(0, nodeList.getLength())
            .mapToObj(i -> (Element) nodeList.item(i));
  }

    public boolean includesDate(Instant date) {
        return date.isAfter(minReportTime.minus(1, ChronoUnit.SECONDS))
            && date.isBefore(maxReportTime.plus(1, ChronoUnit.SECONDS))
            && date.isAfter(minForecastTime.minus(1, ChronoUnit.SECONDS))
            && date.isBefore(maxForecastTime.plus(1, ChronoUnit.SECONDS));
    }

}
