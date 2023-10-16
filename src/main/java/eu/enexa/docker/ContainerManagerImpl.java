package eu.enexa.docker;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.httpclient5.*;


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
    private static final String VOLUME_PATH = System.getenv("ENEXA_SHARED_DIRECTORY");
    private static final String HOST_BASE_PATH = System.getenv("ENEXA_SHARED_DIRECTORY");

    //private static final String NETWORK_NAME = "enexaNet";
    private static final String NETWORK_NAME = System.getenv("DOCKER_NET_NAME");
    private DockerClient dockerClient;

    public ContainerManagerImpl(){
        LOGGER.info("start initiating the ContainerManagerImpl");
        DockerClientConfig standard = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        LOGGER.info("standards are defined");
        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(standard.getDockerHost())
            .sslConfig(standard.getSSLConfig())
            .build();
        LOGGER.info("http client is ready");
        this.dockerClient =  DockerClientImpl.getInstance(standard, httpClient);
        LOGGER.info("docker client is ready");

        // when could not pinging then the exception will be raised
        try {
            this.dockerClient.pingCmd().exec();
        }catch (Exception ex){
            ex.printStackTrace();
            LOGGER.info("docker host uri:"+standard.getDockerHost().toString());
            LOGGER.info("API version:"+standard.getApiVersion().toString());
            LOGGER.info("standard.getSSLConfig()"+standard.getSSLConfig().toString());
        }
        LOGGER.info("docker client is pinged");
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

            //String HOST_PATH = HOST_Base_PATH.concat(expIRI.replace("http://",""));
            String HOST_PATH = HOST_BASE_PATH;
            /*if(!HOST_Base_PATH.endsWith(File.separator)){
                HOST_PATH = HOST_Base_PATH.concat(File.separator.concat(expIRI.replace("http://","")));
            }*/

            List<Bind> allBinds = new ArrayList<>();
            //allBinds.add(new Bind(HOST_PATH, new Volume(VOLUME_PATH+"/"+expIRI.replace("http://",""))));
            //allBinds.add(new Bind(HOST_PATH+"/output", new Volume("/output")));
            allBinds.add(new Bind(HOST_PATH, new Volume("/home/shared")));

            /*if(image.contains("enexa-cel-train-module")){
                allBinds = new ArrayList<>();
                LOGGER.info("in enexa-cel-train-module will use diffrent bindings");
                allBinds.add(new Bind(HOST_PATH, new Volume("/home/shared"),AccessMode.rw));
            }*/

            HostConfig hostConfig = HostConfig
                .newHostConfig()
                .withNetworkMode(NETWORK_NAME)
                .withBinds(allBinds);

            if(image.contains("enexa-dice-embeddings")){
                // increase the size of shared memory for this module
                LOGGER.info("### increase the shared memory to 32 GB ####");
                hostConfig = HostConfig
                    .newHostConfig()
                    .withNetworkMode(NETWORK_NAME)
                    .withBinds(allBinds)
                    .withShmSize(32L * 1024 * 1024 * 1024) ; // 32 GB
            }

            if(image.contains("enexa-cel-train-module")){
                // increase the size of shared memory for this module
                LOGGER.info("### increase the shared memory to 8 GB ####");
                hostConfig = HostConfig
                    .newHostConfig()
                    .withNetworkMode(NETWORK_NAME)
                    .withBinds(allBinds)
                    .withShmSize(8L * 1024 * 1024 * 1024) ; // 8 GB
            }

            String testImage = "";
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
    public String stopContainer(String containerName) {
        try{
            Container c = searchContainerByName(containerName);
            if(c==null){
                return null;
            }
            dockerClient.stopContainerCmd(c.getId()).exec();
            return containerName;
        }catch (DockerException e) {
            return "Error stopping container with Name " + containerName + ": " + e.getMessage();
        }
    }

    @Override
    public String getContainerStatus(String containerId) {
        try{
            Container c = searchContainerByName(containerId);
            if(c==null){
                LOGGER.error("there is no container for this id:"+containerId);
                return null;
            }
            return c.getState();
        } catch (Exception e) {
            LOGGER.error("Got an exception while trying to get the status of \"" + containerId + "\". Returning null.", e);
            return null;
        }
    }

    private Container searchContainerByName(String containerName){
        List<Container> containers = this.dockerClient.listContainersCmd()
            .withShowAll(true)
            .exec();
        if(containers==null){return null;}
        boolean containerExists = false;
        for (Container container : containers) {
            for(String name :container.getNames()) {
                if (name.contains(containerName)) {
                    return container;
                }
            }
        }
        return null;
    }
    private Container searchContainerById(String containerId){
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
