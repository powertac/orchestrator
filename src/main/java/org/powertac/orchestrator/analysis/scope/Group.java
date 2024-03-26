package org.powertac.orchestrator.analysis.scope;

public abstract class Group implements Scope {

    @Override
    public ScopeType getScopeType() {
        return ScopeType.GROUP;
    }

}
