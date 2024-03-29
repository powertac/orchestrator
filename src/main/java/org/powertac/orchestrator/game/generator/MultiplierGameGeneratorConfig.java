package org.powertac.orchestrator.game.generator;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.powertac.orchestrator.game.GameConfig;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

@Entity
@NoArgsConstructor
@AllArgsConstructor
public class MultiplierGameGeneratorConfig extends GameGeneratorConfig {

    public final static String TYPE_ID = "game-multiplier";

    @Setter
    @Getter
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private GameConfig gameConfig;

    @Setter
    @Getter
    private Integer multiplier;

    public String getType() {
        return TYPE_ID;
    }

}
