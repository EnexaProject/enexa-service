package eu.enexa.service;

import org.springframework.stereotype.Component;

public interface ContainerManager {

    public String startContainer(String image);

    public String stopContainer(String containerId);

    public String getContainerStatus(String containerId);
}
