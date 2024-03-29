package org.powertac.orchestrator.game;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer;
import org.powertac.orchestrator.broker.Broker;
import org.powertac.orchestrator.broker.BrokerSet;
import org.powertac.orchestrator.file.File;
import org.powertac.orchestrator.serialization.DeserializationHelper;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class GameDeserializer extends StdNodeBasedDeserializer<Game> {

    public GameDeserializer() {
        super(Game.class);
    }

    @Override
    public Game convert(JsonNode root, DeserializationContext context) throws IOException {
        String id = root.has("id") ? root.get("id").asText() : null;
        String name = root.get("name").asText();
        BrokerSet brokers = parseBrokers(root, context);
        Map<String, String> serverParameters = parseServerParameters(root);
        File bootstrap = root.has("bootstrap") ? parseFile(root.get("bootstrap"), context) : null;
        File seed = root.has("seed") ? parseFile(root.get("seed"), context) : null;
        boolean cancelled = root.has("cancelled") && root.get("cancelled").asBoolean();
        return new Game(id, name, brokers, serverParameters, bootstrap, seed, Instant.now(), cancelled);
    }

    private Map<String, String> parseServerParameters(JsonNode root) {
        Map<String, String> serverParameters = new HashMap<>();
        // FIXME: 'serverParameters' key should be replaced by 'parameters' in the future
        if (root.has("serverParameters")) {
            JsonNode serverParamsNode = root.get("serverParameters");
            serverParamsNode.forEach((node) -> {
                serverParameters.put(node.get("key").asText(), node.get("value").asText());
            });
        } else if (root.has("parameters")) {
            Iterator<Map.Entry<String, JsonNode>> parameterFields = root.get("parameters").fields();
            while (parameterFields.hasNext()) {
                Map.Entry<String, JsonNode> field = parameterFields.next();
                serverParameters.put(field.getKey(), field.getValue().asText());
            }
        }
        return serverParameters;
    }

    private BrokerSet parseBrokers(JsonNode root, DeserializationContext context) throws IOException {
        Set<Broker> brokers = root.has("brokers")
            ? DeserializationHelper.deserializeSet(root.get("brokers"), Broker.class, context)
            : new HashSet<>();
        return new BrokerSet(
            UUID.randomUUID().toString(),
            brokers);
    }

    private File parseFile(JsonNode fileNode, DeserializationContext context) throws IOException {
        return DeserializationHelper.defaultDeserialize(fileNode, File.class, context);
    }

}
