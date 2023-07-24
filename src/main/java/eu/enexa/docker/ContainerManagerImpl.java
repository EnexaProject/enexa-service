package eu.enexa.docker;

import eu.enexa.service.ContainerManager;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.List;

@Component
public class ContainerManagerImpl implements ContainerManager {
    @Override
    public String startContainer(String image, String podName, List<AbstractMap.SimpleEntry<String, String>> variables) {
        return null;
    }

    @Override
    public String stopContainer(String podName) {
        return null;
    }

    @Override
    public String getContainerStatus(String podName) {
        return null;
    }
}
