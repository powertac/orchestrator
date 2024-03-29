package org.powertac.orchestrator.broker;

import org.powertac.orchestrator.api.stomp.EntityPublisher;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PersistentBrokerRepository implements BrokerRepository {

    private final BrokerCrudRepository brokers;
    private final EntityPublisher<Broker> publisher;

    public PersistentBrokerRepository(BrokerCrudRepository brokers, EntityPublisher<Broker> publisher) {
        this.brokers = brokers;
        this.publisher = publisher;
    }

    @Override
    public Collection<Broker> findAll() {
        List<Broker> brokerList = new ArrayList<>();
        brokers.findAll().forEach(brokerList::add);
        return brokerList;
    }

    @Override
    public Optional<Broker> findById(String id) {
        return brokers.findById(id);
    }

    @Override
    public Broker findByNameAndVersion(String name, String version) {
        return brokers.findByNameAndVersion(name, version);
    }

    @Override
    public boolean exists(String id) {
        return brokers.existsById(id);
    }

    @Override
    public boolean exists(String name, String version) {
        return brokers.existsByNameAndVersion(name, version);
    }

    @Override
    public void save(Broker broker) throws BrokerConflictException {
        Broker persistedBroker = brokers.findByNameAndVersion(broker.getName(), broker.getVersion());
        if (null != persistedBroker && !persistedBroker.getId().equals(broker.getId())) {
            throw new BrokerConflictException(String.format(
                "cannot save new broker[name=%s, version=%s] due to conflict with existing broker[id=%s]",
                broker.getName(),
                broker.getVersion(),
                persistedBroker.getId()));
        }
        if (null == broker.getId()) {
            broker.setId(UUID.randomUUID().toString());
        }
        brokers.save(broker);
        publisher.publish(broker);
    }

}
