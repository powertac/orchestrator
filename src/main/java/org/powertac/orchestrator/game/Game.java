package org.powertac.orchestrator.game;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import lombok.*;
import org.powertac.orchestrator.analysis.scope.Scope;
import org.powertac.orchestrator.baseline.Baseline;
import org.powertac.orchestrator.broker.Broker;
import org.powertac.orchestrator.broker.BrokerSet;
import org.powertac.orchestrator.file.File;
import org.powertac.orchestrator.treatment.Treatment;
import org.powertac.orchestrator.util.InstantToNumberSerializer;
import org.powertac.orchestrator.weather.WeatherConfiguration;

import java.time.Instant;
import java.util.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property="id")
@Builder
public class Game implements Scope {

    enum ExecutionStatus {
        NONE,
        RUNNING,
        COMPLETED,
        FAILED
    }

    enum QueueStatus {
        QUEUED,
        PAUSED,
        CANCELLED
    }

    @Getter
    @Setter
    @Id
    @Column(length = 36)
    private String id;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private BrokerSet brokerSet;

    @Getter
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_server_parameters", joinColumns = {@JoinColumn(name = "game_id", referencedColumnName = "id")})
    @MapKeyColumn(name = "parameter", length = 128)
    @Column(name = "value")
    @Builder.Default
    private Map<String, String> serverParameters = new HashMap<>();

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Deprecated // TODO : remove; bootstrap should always be created on a per game basis
    private File bootstrap; // TODO : make getter return Optional

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Deprecated // TODO : use 'base' property instead
    private File seed; // TODO : make getter return Optional

    @Getter
    @Setter
    @JsonSerialize(using = InstantToNumberSerializer.class)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Getter
    @Setter
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "game", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<GameRun> runs = new ArrayList<>();

    @Getter
    @Setter
    @Builder.Default
    @Deprecated // TODO : use 'status' property instead
    private boolean cancelled = false;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private WeatherConfiguration weatherConfiguration;

    @Getter
    @Setter
    @ManyToOne
    private Baseline baseline; // TODO : make getter return Optional

    @Getter
    @Setter
    @ManyToOne
    private Treatment treatment; // TODO : make getter return Optional

    @Getter
    @Setter
    @ManyToOne
    private Game base; // TODO : make getter return Optional

    @Getter
    @Setter
    @Column(length = 2)
    private QueueStatus queueStatus = QueueStatus.PAUSED;

    @Getter
    @Column(length = 2)
    private ExecutionStatus executionStatus = ExecutionStatus.NONE;

    @PrePersist
    public void updateExecutionStatus() {
        if (runs.size() < 1) {
            executionStatus = ExecutionStatus.NONE;
        } else if (isRunning()) {
            executionStatus = ExecutionStatus.RUNNING;
        } else if (null != getLatestSuccessfulRun()) {
            executionStatus = ExecutionStatus.COMPLETED;
        } else if (hasFailed()) {
            executionStatus = ExecutionStatus.FAILED;
        }
    }

    // TODO : replace constructors with factory methods
    @Deprecated
    public Game(String id, String name, BrokerSet brokers, Map<String, String> serverParameters, File bootstrap, File seed, Instant createdAt) {
        this(id,
            name,
            brokers,
            serverParameters,
            bootstrap,
            seed,
            createdAt,
            new ArrayList<>(),
            false,
            null,
            null,
            null,
            null,
            QueueStatus.PAUSED,
            ExecutionStatus.NONE);
    }

    @Deprecated
    public Game(String id, String name, BrokerSet brokers, Map<String, String> serverParameters, File bootstrap, File seed, Instant createdAt, boolean cancelled) {
        this(id,
            name,
            brokers,
            serverParameters,
            bootstrap,
            seed,
            createdAt,
            new ArrayList<>(),
            cancelled,
            null,
            null,
            null,
            null,
            QueueStatus.PAUSED,
            ExecutionStatus.NONE);
    }

    @Deprecated
    public Game(String id, String name, BrokerSet brokers, Map<String, String> serverParameters, Instant createdAt, boolean cancelled) {
        this(id,
            name,
            brokers,
            serverParameters,
            null,
            null,
            createdAt,
            new ArrayList<>(),
            cancelled,
            null,
            null,
            null,
            null,
            QueueStatus.PAUSED,
            ExecutionStatus.NONE);
    }

    public Set<Broker> getBrokers() {
        return brokerSet.getBrokers();
    }

    @Transient
    public boolean isRunning() {
        return runs.stream()
            .map(GameRun::isRunning)
            .reduce(false, (oneIsRunning, currentOneIsRunning) -> oneIsRunning || currentOneIsRunning);
    }

    @Transient
    @Deprecated
    public GameRun getLatestSuccessfulRun() { // FIXME : this doesn't work correctly!!!
        return runs.stream().reduce(null, (current, run) ->
           run.wasSuccessful()
               ? null == current || run.getEnd().isAfter(current.getEnd()) ? run : current
               : null
        );
    }

    @Transient
    public Optional<GameRun> getSuccessfulRun() {
        return runs.stream()
            .filter(GameRun::wasSuccessful)
            .max(Comparator.comparing(GameRun::getEnd))
            .stream().findFirst();
    }

    @Transient
    public boolean hasFailed() {
        return !isRunning()
            && runs.size() > 2
            && null == getLatestSuccessfulRun();
    }

    @Override
    @Transient
    public Collection<Game> getGames() {
        return Set.of(this);
    }

    public GameConfigDTO getConfigDto() {
        return GameConfigDTO.builder()
            .brokerIds(brokerSet.getIds())
            .parameters(serverParameters)
            .weather(weatherConfiguration)
            .build();
    }

}
