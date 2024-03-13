package org.powertac.orchestrator.baseline;

import lombok.*;
import org.powertac.orchestrator.analysis.scope.GameGroup;
import org.powertac.orchestrator.broker.BrokerSet;
import org.powertac.orchestrator.game.Game;
import org.powertac.orchestrator.game.generator.GameGeneratorConfig;
import org.powertac.orchestrator.weather.WeatherConfiguration;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Baseline extends GameGroup {

    @Id
    @Getter
    @Setter
    @Column(length = 36)
    private String id;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "baseline_parameters", joinColumns = {@JoinColumn(name = "baseline_id", referencedColumnName = "id")})
    @MapKeyColumn(name = "parameter", length = 128)
    @Column(name = "value")
    private Map<String, String> commonParameters;

    @Getter
    @Setter
    @OrderColumn
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<BrokerSet> brokerSets;

    @Getter
    @Setter
    @OrderColumn
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<WeatherConfiguration> weatherConfigurations;

    @Getter
    @Setter
    @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE}, mappedBy = "baseline")
    @OrderColumn
    private List<Game> games;

    @Getter
    @Setter
    private Instant createdAt;

    @Getter
    @Setter
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private GameGeneratorConfig config;

}
