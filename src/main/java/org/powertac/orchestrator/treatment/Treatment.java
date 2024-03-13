package org.powertac.orchestrator.treatment;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.powertac.orchestrator.analysis.scope.GameGroup;
import org.powertac.orchestrator.baseline.Baseline;
import org.powertac.orchestrator.game.Game;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Treatment extends GameGroup {

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
    @ManyToOne
    @JsonIgnore
    private Baseline baseline;

    @Getter
    @Setter
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Modifier modifier;

    @Getter
    @Setter
    private Instant createdAt;

    @Getter
    @Setter
    @OneToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE}, mappedBy = "treatment")
    @OrderColumn
    @JsonIgnore
    private List<Game> games;

    @JsonGetter
    public String getBaselineId() {
        return baseline.getId();
    }

    @JsonGetter
    public List<String> getGameIds() {
        return games.stream()
            .map(Game::getId)
            .collect(Collectors.toList());
    }

}
