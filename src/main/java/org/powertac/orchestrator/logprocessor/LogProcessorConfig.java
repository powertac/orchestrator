package org.powertac.orchestrator.logprocessor;

import org.powertac.orchestrator.docker.DockerContainerController;
import org.powertac.orchestrator.docker.DockerImageRepository;
import org.powertac.orchestrator.paths.PathProvider;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogProcessorConfig implements ApplicationContextAware {

    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

    @Deprecated
    public LogProcessorProvider configurableProvider() {
        final ConfigurableLogProcessorProvider provider = new ConfigurableLogProcessorProvider();
        provider.addProcessor(new LogProcessor(
            "weather-reports",
            "org.powertac.logprocessor.weather.WeatherReports",
            "%s.weather-reports.csv"));
        provider.addProcessor(new LogProcessor(
            "tariff-transactions",
            "org.powertac.logprocessor.tariff.TariffTransactions",
            "%s.tariff-transactions.csv"));
        provider.addProcessor(new LogProcessor(
            "balancing-market-transactions",
            "org.powertac.logprocessor.market.BalancingMarketTransactions",
            "%s.balancing-market-transactions.csv"));
        provider.addProcessor(new LogProcessor(
            "broker-market-prices",
            "org.powertac.logprocessor.broker.BrokerMarketPrices",
            "%s.broker-market-prices.csv"));
        return provider;
    }

    @Bean
    public LogProcessorProvider logProcessorProvider() {
        return new ContainerReflectionLogProcessorProvider(
            context.getBean(DockerImageRepository.class),
            context.getBean(LogProcessorReflectionContainerCreator.class),
            context.getBean(DockerContainerController.class),
            context.getBean(PathProvider.class));
    }

}
