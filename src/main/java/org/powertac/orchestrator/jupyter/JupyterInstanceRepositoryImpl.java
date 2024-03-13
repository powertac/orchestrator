package org.powertac.orchestrator.jupyter;

import org.powertac.orchestrator.analysis.scope.Scope;
import org.powertac.orchestrator.docker.ContainerCreator;
import org.powertac.orchestrator.docker.DockerContainer;
import org.powertac.orchestrator.docker.DockerContainerRepository;
import org.powertac.orchestrator.docker.exception.ContainerConflictException;
import org.powertac.orchestrator.util.PortPool;
import org.powertac.orchestrator.util.exception.ConflictException;
import org.powertac.orchestrator.util.exception.CreationException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JupyterInstanceRepositoryImpl implements JupyterInstanceRepository {

    private final JupyterTokenFactory tokens;
    private final ContainerCreator<JupyterInstance> containerCreator;
    private final DockerContainerRepository containers;
    private final PortPool portPool;

    private final Map<String, JupyterInstance> instances;

    public JupyterInstanceRepositoryImpl(JupyterTokenFactory tokens, ContainerCreator<JupyterInstance> containerCreator,
                                         DockerContainerRepository containers, PortPool portPool) {
        this.tokens = tokens;
        this.containerCreator = containerCreator;
        this.containers = containers;
        this.portPool = portPool;
        instances = new HashMap<>();
    }

    @Override
    public JupyterInstance create(Scope scope) throws ConflictException, CreationException {
        if (find(scope.getId()).isPresent()) {
            throw new ConflictException("jupyter instance for scope with id=" + scope.getId() + " already exists");
        } else {
            try {
                Integer port = portPool.claimNext();
                try {
                    String token = tokens.createToken(scope.getId(), port);
                    JupyterInstance instance = new JupyterInstance(scope, port, token);
                    instances.put(instance.getId(), instance);
                    return instance;
                } catch (Exception e) {
                    portPool.release(port);
                    throw new CreationException("unable to create new jupyter instance for scope with id=" + scope.getId(), e);
                }
            } catch (PortPool.PoolDepletedException e) {
                throw new CreationException("unable to allocate port for new jupyter instance  with scopeId=" + scope.getId(), e);
            }
        }
    }

    @Override
    public Optional<JupyterInstance> find(String scopeId) {
        if (instances.containsKey(scopeId)) {
            return Optional.of(instances.get(scopeId));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public JupyterInstance run(JupyterInstance instance) throws ConflictException {
        try {
            DockerContainer container = containerCreator.createFor(instance);
            containers.add(container);
            containers.run(container);
            instance.setContainer(container);
            return instance;
        } catch (ContainerConflictException e) {
            throw new ConflictException(e);
        }
    }

    @Override
    public void remove(JupyterInstance instance) {
        if (instances.containsKey(instance.getId())) {
            if (null != instance.getContainer()) {
                containers.remove(instance.getContainer());
            }
            portPool.release(instance.getPort());
            instances.remove(instance.getId());
        }
    }

    @Override
    public JupyterInstance update(JupyterInstance instance) throws NotFoundException {
        if (!instances.containsKey(instance.getId())) {
            throw new NotFoundException(instance.getId());
        }
        DockerContainer updatedContainer = containers.update(instance.getContainer());
        instance.setContainer(updatedContainer);
        return instance;
    }

}
