package org.powertac.orchestrator.jupyter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TransientJupyterTokenFactory implements JupyterTokenFactory {

    private final String salt;

    public TransientJupyterTokenFactory(String salt) {
        this.salt = salt;
    }

    @Override
    public String createToken(String id, Integer port) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt.getBytes());
        byte[] bytes = digest.digest((id + port).getBytes());
        return decimalToHexString(bytes);
    }

    private String decimalToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

}
