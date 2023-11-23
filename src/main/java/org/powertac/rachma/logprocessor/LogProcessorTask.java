package org.powertac.rachma.logprocessor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.powertac.rachma.docker.ContainerTask;
import org.powertac.rachma.game.Game;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import java.util.Set;

@Entity
@SuperBuilder
@NoArgsConstructor
public class LogProcessorTask extends ContainerTask {

    @Getter
    @ManyToOne
    private Game game;

    @Getter
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> processorIds;

}
