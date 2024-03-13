package org.powertac.orchestrator.jupyter;

import org.powertac.orchestrator.analysis.scope.Scope;
import org.powertac.orchestrator.util.exception.ConflictException;
import org.powertac.orchestrator.util.exception.CreationException;

import java.util.Optional;

public interface JupyterInstanceRepository {

    JupyterInstance create(Scope scope) throws CreationException, ConflictException;

    Optional<JupyterInstance> find(String scopeId);

    JupyterInstance run(JupyterInstance instance) throws ConflictException;

    JupyterInstance update(JupyterInstance instance) throws NotFoundException;

    void remove(JupyterInstance instance);

    class NotFoundException extends Exception {

        private final String id;

        public NotFoundException(String id) {
            this.id = id;
        }

        @Override
        public String getMessage() {
            return "cannot find jupyter instance with id=" + id;
        }

    }

}
