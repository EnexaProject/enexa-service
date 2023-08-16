package eu.enexa.docker;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
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

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

//TODO: remove primary after using config for beans
@Primary
@Component("dockerContainerManager")
public class ContainerManagerImpl implements ContainerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManagerImpl.class);
    private static final String VOLUME_PATH = "";
    private static final String HOST_Base_PATH = System.getenv("ENEXA_SHARED_DIRECTORY");
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
            image = trimImageName(image);

            List<Volume> volumes = new ArrayList<>();
            String expIRI="";
            for(AbstractMap.SimpleEntry v:variables){
                if(v.getKey().toString().equals("ENEXA_EXPERIMENT_IRI")){
                    expIRI = v.getValue().toString();
                }
            }
            if(expIRI.equals("")||expIRI.length()<10){
                LOGGER.warn("ENEXA_EXPERIMENT_IRI is null or less than 10 character");
            }

            String HOST_PATH = HOST_Base_PATH.concat(expIRI.replace("http://",""));
            if(!HOST_Base_PATH.endsWith(File.separator)){
                HOST_PATH = HOST_Base_PATH.concat(File.separator.concat(expIRI.replace("http://","")));
            }

            List<Bind> allBinds = new ArrayList<>();
            allBinds.add(new Bind(HOST_PATH, new Volume(VOLUME_PATH+"/"+expIRI.replace("http://",""))));
            allBinds.add(new Bind(HOST_PATH+"/output", new Volume("/output")));

            HostConfig hostConfig = HostConfig
                .newHostConfig()
                .withNetworkMode("enexaNet")
                .withBinds(allBinds);

            CreateContainerResponse container = dockerClient.createContainerCmd(image)
                .withName(containerName)
                .withEnv(mapToEnvironmentArray(variables))
                .withHostConfig(hostConfig)
                .exec();

            //.withVolumes(Volume.parse(VOLUME_PATH))

            dockerClient.startContainerCmd(container.getId()).exec();

            return container.getId();
        }else{
            throw new RuntimeException("there is no docker clients exist");
        }
    }

    private String trimImageName(String image) {
        String[] parts = image.split("docker:image:");
        if(parts.length<2){return image;}
        return parts[1];
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
