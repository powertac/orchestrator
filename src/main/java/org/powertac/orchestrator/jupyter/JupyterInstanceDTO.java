package org.powertac.orchestrator.jupyter;

public record JupyterInstanceDTO(String scopeId, Integer port, String token, Boolean isRunning) {}
