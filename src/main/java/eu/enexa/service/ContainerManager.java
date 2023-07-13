package eu.enexa.service;

public interface ContainerManager {

    public String startContainer(String image, String podName);

    public String stopContainer(String containerId);

    public String getContainerStatus(String containerId);
}
