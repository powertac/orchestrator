package org.powertac.orchestrator.logprocessor;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer;

public class LogProcessorDeserializer extends StdNodeBasedDeserializer<LogProcessor> {

    public LogProcessorDeserializer() {
        super(LogProcessor.class);
    }

    @Override
    public LogProcessor convert(JsonNode node, DeserializationContext context) {
        String extension = !node.get("extension").isNull() ? node.get("extension").asText() : null;
        return new LogProcessor(
            node.get("name").asText(),
            node.get("class").asText(),
            extension != null ? "%s" + extension : null);
    }

}
