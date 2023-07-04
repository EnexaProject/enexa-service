package eu.enexa.service;

public interface ContainerManager {

    public String startContainer(String image);
    
    public String stopContainer(String containerId);
    
    public String getContainerStatus(String containerId);
}
