package org.powertac.orchestrator.game;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;
import org.powertac.orchestrator.broker.Broker;
import org.powertac.orchestrator.broker.BrokerSet;
import org.powertac.orchestrator.file.File;
import org.powertac.orchestrator.server.SimulationServerVersion;
import org.powertac.orchestrator.weather.WeatherConfiguration;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize(using = GameConfigDeserializer.class)
public class GameConfig {

    @Id
    @Getter
    @Setter
    @Column(length = 36)
    private String id;

    @Getter
    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private BrokerSet brokers;

    @Getter
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_config_parameters", joinColumns = {@JoinColumn(name = "game_config_id", referencedColumnName = "id")})
    @MapKeyColumn(name = "parameter", length = 128)
    @Column(name = "value")
    private Map<String, String> parameters = new HashMap<>();

    @Getter
    @Setter
    @OneToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private WeatherConfiguration weather;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.EAGER)
    private SimulationServerVersion serverVersion;

    @Getter
    @Builder.Default
    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "game_config_binds",
        joinColumns = {@JoinColumn(name = "game_config_id", referencedColumnName = "id")},
        inverseJoinColumns = {@JoinColumn(name = "file_id", referencedColumnName = "id")})
    @MapKeyJoinColumn(name = "broker_id")
    private Map<Broker, File> binds = new HashMap<>();

    @Getter
    @Setter
    private Instant createdAt;

}
