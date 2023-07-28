package eu.enexa.docker;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.httpclient5.*;
import com.github.dockerjava.api.model.Container;


import com.github.dockerjava.core.DockerClientImpl;
import eu.enexa.service.ContainerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

//TODO: remove primary after using config for beans
@Primary
@Component("dockerContainerManager")
public class ContainerManagerImpl implements ContainerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManagerImpl.class);
    private static final String VOLUME_PATH = "/enexa";
    private DockerClient dockerClient;

    public ContainerManagerImpl(){
        DockerClientConfig standard = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(standard.getDockerHost())
            .sslConfig(standard.getSSLConfig())
            .build();

        this.dockerClient =  DockerClientImpl.getInstance(standard, httpClient);

        // when could not pinging then the exception will be raised
        this.dockerClient.pingCmd().exec();
    }

    @Override
    public String startContainer(String image, String containerName, List<AbstractMap.SimpleEntry<String, String>> variables) {
        if (variables == null ){
            variables = new ArrayList<>();
        }
        if(this.dockerClient!=null) {
            CreateContainerResponse container = dockerClient.createContainerCmd(image)
                .withName(containerName)
                .withEnv(mapToEnvironmentArray(variables))
                .withVolumes(Volume.parse(VOLUME_PATH))
                .exec();

            dockerClient.startContainerCmd(container.getId()).exec();

            return container.getId();
        }else{
            throw new RuntimeException("there is no docker clients exist");
        }
    }

    private String[] mapToEnvironmentArray(List<AbstractMap.SimpleEntry<String, String>> environmentVariables) {
        if (environmentVariables == null || environmentVariables.isEmpty()) {
            return new String[0];
        }

        return environmentVariables.stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .toArray(String[]::new);
    }

    @Override
    public String stopContainer(String containerId) {
        try{
            //TODO: we can remove the search part and stop just based on containerId
            Container c = searchContainer(containerId);
            if(c==null){
                return null;
            }
            dockerClient.stopContainerCmd(c.getId()).exec();
            return containerId;
        }catch (DockerException e) {
            return "Error stopping container with ID " + containerId + ": " + e.getMessage();
        }
    }

    @Override
    public String getContainerStatus(String containerId) {
        try{
            Container c = searchContainer(containerId);
            if(c==null){
                return null;
            }
            return c.getState();
        } catch (Exception e) {
            LOGGER.error("Got an exception while trying to get the status of \"" + containerId + "\". Returning null.", e);
            return null;
        }
    }

    private Container searchContainer(String containerId){
        List<Container> containers = this.dockerClient.listContainersCmd()
            .withShowAll(true)
            .exec();
        if(containers==null){return null;}
        boolean containerExists = false;
        for (Container container : containers) {
            if (container.getId().equals(containerId)) {
                return container;
            }
        }
        return null;
    }
}
