package org.powertac.orchestrator.jupyter;

import org.powertac.orchestrator.docker.ContainerCreator;
import org.powertac.orchestrator.docker.DockerContainerRepository;
import org.powertac.orchestrator.util.MultipleRangePortPool;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

@Configuration
public class JupyterConfig implements ApplicationContextAware {

    private final static String randomNumberAlgorithm = "SHA1PRNG";

    @Value("${services.analysis.port-ranges}")
    private String configuredPortRanges;

    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Bean
    public JupyterInstanceRepository jupyterInstanceRepository() {
        ResolvableType containerCreatorType = ResolvableType.forClassWithGenerics(ContainerCreator.class, JupyterInstance.class);
        return new JupyterInstanceRepositoryImpl(
            context.getBean(JupyterTokenFactory.class),
            (ContainerCreator<JupyterInstance>) context.getBeanProvider(containerCreatorType).getObject(),
            context.getBean(DockerContainerRepository.class),
            new MultipleRangePortPool(configuredPortRanges));
    }

    @Bean
    public JupyterTokenFactory jupyterTokenFactory() throws NoSuchAlgorithmException {
        return new TransientJupyterTokenFactory(createSalt());
    }

    private String createSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance(randomNumberAlgorithm);
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return Arrays.toString(salt);
    }

}
