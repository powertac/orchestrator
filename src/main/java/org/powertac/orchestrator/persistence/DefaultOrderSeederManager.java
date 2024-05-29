package org.powertac.orchestrator.persistence;

import org.powertac.orchestrator.server.SimulationServerVersionSeeder;
import org.powertac.orchestrator.user.DefaultUserSeeder;
import org.powertac.orchestrator.user.UserRoleSeeder;
import org.springframework.stereotype.Service;

@Service
public class DefaultOrderSeederManager implements SeederManager {

    private final UserRoleSeeder roleSeeder;
    private final DefaultUserSeeder userSeeder;
    private final SimulationServerVersionSeeder serverVersionSeeder;

    public DefaultOrderSeederManager(UserRoleSeeder roleSeeder, DefaultUserSeeder userSeeder,
                                     SimulationServerVersionSeeder serverVersionSeeder) {
        this.roleSeeder = roleSeeder;
        this.userSeeder = userSeeder;
        this.serverVersionSeeder = serverVersionSeeder;
    }

    @Override
    public void runSeeders() throws SeederException {
        roleSeeder.seed();
        userSeeder.seed();
        serverVersionSeeder.seed();
    }

}
