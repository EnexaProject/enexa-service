package eu.enexa.service;

import java.util.AbstractMap;
import java.util.List;

public interface ContainerManager {

    public String startContainer(String image, String podName, List<AbstractMap.SimpleEntry<String,String>> variables);

    public String stopContainer(String podName);

    public String getContainerStatus(String podName);
}
