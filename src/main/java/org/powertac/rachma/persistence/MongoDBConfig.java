package org.powertac.rachma.persistence;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.rachma.broker.BrokerRepository;
import org.powertac.rachma.file.PathProvider;
import org.powertac.rachma.game.GameRepository;
import org.powertac.rachma.job.JobRepository;
import org.powertac.rachma.job.PersistentJobRepository;
import org.powertac.rachma.job.serialization.JobReadConverter;
import org.powertac.rachma.job.serialization.JobWriteConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@Deprecated
@ConditionalOnProperty(value = "persistence.legacy.enable-mongo", havingValue = "true")
public class MongoDBConfig extends AbstractMongoConfiguration implements ApplicationContextAware {

    private static final int defaultMongoPort = 27017;

    @Setter
    private ApplicationContext applicationContext;

    @Value("${persistence.mongodb.host}")
    private String mongoHost;

    @Value("${persistence.mongodb.port}")
    private Integer mongoPort;

    @Value("${spring.data.mongodb.username}")
    private String username;

    @Value("${spring.data.mongodb.password}")
    private String password;

    private final List<Class<? extends Converter<?,?>>> converterClasses = Stream.of(
        JobWriteConverter.class,
        JobReadConverter.class
    ).collect(Collectors.toList());

    @Override
    protected String getDatabaseName() {
        return "powertac";
    }

    @Override
    public MongoClient mongoClient() {
        return new MongoClient(
            getServerAddresses(),
            getCredential(),
            getClientOptions());
    }

    @Override
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(getConverters());
    }

    @Override
    public MappingMongoConverter mappingMongoConverter() throws Exception {
        MappingMongoConverter mongoConverter = super.mappingMongoConverter();
        mongoConverter.setMapKeyDotReplacement("_");
        return mongoConverter;
    }

    private List<Converter<?,?>> getConverters() {
        List<Converter<?,?>> converters = new ArrayList<>();
        for (Class<? extends Converter<?,?>> converterClass : converterClasses) {
            Converter<?,?> converter = applicationContext.getBean(converterClass);
            converters.add(converter);
        }
        return converters;
    }

    private List<ServerAddress> getServerAddresses() {
        ServerAddress serverAddress = new ServerAddress(mongoHost, getMongoPort());
        return Collections.singletonList(serverAddress);
    }

    private int getMongoPort() {
        return null != mongoPort ? mongoPort : defaultMongoPort;
    }

    private MongoCredential getCredential() {
        return MongoCredential.createScramSha256Credential(username, getDatabaseName(), password.toCharArray());
    }

    private MongoClientOptions getClientOptions() {
        return MongoClientOptions.builder().build();
    }

}
