package eu.enexa.config;

import eu.enexa.service.ContainerManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

//TODO use this config
//@Configuration
public class Config {
    //@Bean
    public ContainerManager getContainerManager() {
            return new eu.enexa.docker.ContainerManagerImpl();
    }
}
