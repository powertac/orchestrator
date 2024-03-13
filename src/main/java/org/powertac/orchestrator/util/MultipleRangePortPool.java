package org.powertac.orchestrator.util;

import org.powertac.orchestrator.util.exception.ConfigurationException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MultipleRangePortPool implements PortPool {

    private final Map<Integer, Integer> portRanges;
    private final HashSet<Integer> claimedPorts;

    /**
     * @param portRangesRepresentation  comma-separated list of port ranges in the format "minPort:maxPort"
     */
    public MultipleRangePortPool(String portRangesRepresentation) {
        portRanges = parsePortRanges(portRangesRepresentation.split(","));
        claimedPorts = new HashSet<>();
    }

    @Override
    public int claimNext() throws PoolDepletedException {
        for (Map.Entry<Integer, Integer> range : portRanges.entrySet()) {
            for (int i = range.getKey(); i <= range.getValue(); i++) {
                if (!claimedPorts.contains(i)) {
                    claimedPorts.add(i);
                    return i;
                }
            }
        }
        throw new PoolDepletedException(portRanges);
    }

    @Override
    public void release(int port) {
        claimedPorts.remove(port);
    }

    private Map<Integer, Integer> parsePortRanges(String[] portRangeRepresentations) {
        Map<Integer, Integer> ranges = new HashMap<>();
        for (String range : portRangeRepresentations) {
            String[] ports = range.split(":");
            if (ports.length != 2) {
                throw new ConfigurationException("invalid port range configuration: port ranges must (only) consist of minPort and maxPort");
            } else {
                Integer minPort = Integer.valueOf(ports[0]);
                Integer maxPort = Integer.valueOf(ports[1]);
                if (minPort > maxPort) {
                    throw new ConfigurationException("invalid port range configuration: maxPort must be greater than or equal to minPort");
                }
                ranges.put(minPort, maxPort);
            }
        }
        if (ranges.isEmpty()) {
            throw new RuntimeException("no port ranges defined for port pool");
        }
        return ranges;
    }

}
