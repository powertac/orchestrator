package org.powertac.orchestrator.util;

import java.util.Map;
import java.util.stream.Collectors;

public interface PortPool {

    int claimNext() throws PoolDepletedException;
    void release(int port);

    class PoolDepletedException extends Exception {

        Map<Integer, Integer> ranges;

        public PoolDepletedException(Map<Integer, Integer> ranges) {
            this.ranges = ranges;
        }

        @Override
        public String getMessage() {
            String rangeList = ranges.entrySet().stream()
                .map(r -> r.getKey() + ":" + r.getValue())
                .collect(Collectors.joining(","));
            return "no free ports left in pool with ranges " + rangeList;
        }

    }

}
