package org.powertac.orchestrator.analysis;

import org.powertac.orchestrator.logprocessor.LogProcessorProvider;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class AnalyzerConfig implements ApplicationContextAware {

    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    @Bean
    public AnalyzerProvider analyzerProvider() {
        ConfigurableAnalyzerProvider analyzerProvider = new ConfigurableAnalyzerProvider();
        LogProcessorProvider processorProvider = context.getBean(LogProcessorProvider.class);
        /*analyzerProvider.addAnalyzer(Analyzer.builder()
                .name("wholesale-prices-boxplot")
                .requirements(Set.of(processorProvider.get("broker-market-prices")))
                .scope(AnalyzerScope.GROUP)
                .build());
         */
        return analyzerProvider;
    }

}
