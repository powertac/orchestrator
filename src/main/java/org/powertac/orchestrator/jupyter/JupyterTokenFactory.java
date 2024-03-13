package org.powertac.orchestrator.jupyter;

import java.security.NoSuchAlgorithmException;

public interface JupyterTokenFactory {

    String createToken(String id, Integer port) throws NoSuchAlgorithmException;

}
