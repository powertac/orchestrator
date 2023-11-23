package org.powertac.rachma.baseline;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.powertac.rachma.game.generator.GameGeneratorConfig;
import org.powertac.rachma.game.generator.GameGeneratorConfigDeserializer;

@NoArgsConstructor
public class NewBaselineDTO {

    @Getter
    private String name;

    @Getter
    @JsonDeserialize(using = GameGeneratorConfigDeserializer.class)
    private GameGeneratorConfig generator;

}