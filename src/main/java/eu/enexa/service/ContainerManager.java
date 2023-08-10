package eu.enexa.service;

import java.util.AbstractMap;
import java.util.List;

public interface ContainerManager {

    // containerName is the podName for kubernetes
    public String startContainer(String image, String containerName, List<AbstractMap.SimpleEntry<String,String>> variables);

    public String stopContainer(String podName);

    public String getContainerStatus(String podName);
}
