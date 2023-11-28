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
import java.util.UUID;
import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

//TODO: remove primary after using config for beans
@Primary
@Component("dockerContainerManager")
public class ContainerManagerImpl implements ContainerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManagerImpl.class);
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

    private String makeTheDirectoryInThisPath(String partOneOfPath, String partTwoOfPath) {
        String path = combinePaths(partOneOfPath, partTwoOfPath);
        File appPathDirectory = new File(path);
        if(!appPathDirectory.exists()){
            appPathDirectory.mkdirs();
        }
        return path;
    }

    public static String combinePaths(String partOne, String partTwo) {
        String path = partOne + File.separator + partTwo;
        if (partOne.endsWith(File.separator)) {
            path = partOne + partTwo;
        }
        return path;
    }

    @Override
    public String startContainer(String image, String containerName, List<AbstractMap.SimpleEntry<String, String>> variables,String hostSharedDirectory, String appName) {
        if (variables == null ){
            variables = new ArrayList<>();
        }
        if(this.dockerClient!=null) {
            image = trimImageName(image);

            String expIRI="";

            for(AbstractMap.SimpleEntry v:variables){
                if(v.getKey().toString().equals("ENEXA_EXPERIMENT_IRI")){
                    expIRI = v.getValue().toString();
                }
            }
            if(expIRI.equals("")||expIRI.length()<10){
                LOGGER.warn("ENEXA_EXPERIMENT_IRI is null or less than 10 character");
            }

            // this hardcoded path should convert to environment variable if used somewhere else
            String containerSharedDirectory = "/home/shared";
            String containerBasePath = makeTheDirectoryInThisPath(containerSharedDirectory,appName);
            String hostBasePath = makeTheDirectoryInThisPath(hostSharedDirectory,appName);

            String writeableDirectory =expIRI.split("/")[expIRI.split("/").length - 1];
            String hostWritablePath = makeTheDirectoryInThisPath(hostBasePath,writeableDirectory);
            String containerWritablePath = combinePaths(containerBasePath,writeableDirectory);
            String moduleInstanceIRI = UUID.randomUUID().toString();
            //String hostModuleInstancePath = makeTheDirectoryInThisPath(hostWritablePath,moduleInstanceIRI);
            String containerModuleInstancePath = combinePaths(containerWritablePath, moduleInstanceIRI);

            variables.add(new AbstractMap.SimpleEntry<>("ENEXA_SHARED_DIRECTORY", containerSharedDirectory));
            variables.add(new AbstractMap.SimpleEntry<>("ENEXA_MODULE_INSTANCE_DIRECTORY", containerModuleInstancePath));
            variables.add(new AbstractMap.SimpleEntry<>("ENEXA_WRITEABLE_DIRECTORY",containerWritablePath));
            List<Bind> allBinds = new ArrayList<>();

            // until Module instance is the subdirectory of writeable directory , we do not need this
            //allBinds.add(new Bind(hostModuleInstancePath, new Volume(containerModuleInstancePath),AccessMode.rw));

            // Order is important here , If add something check it

            allBinds.add(new Bind(hostWritablePath, new Volume(containerWritablePath),AccessMode.rw));
            allBinds.add(new Bind(hostSharedDirectory, new Volume(containerSharedDirectory),AccessMode.ro));

            LOGGER.info("this path from host :"+hostSharedDirectory+" mapped to this path in container"+containerBasePath);
            LOGGER.info("this path from host :"+hostWritablePath+" mapped to this path in container "+containerWritablePath);
            //LOGGER.info("this path from host :"+hostModuleInstancePath+" mapped to this path in container "+containerModuleInstancePath);

            HostConfig dockerHostConfig = HostConfig
                .newHostConfig()
                .withNetworkMode(NETWORK_NAME)
                .withBinds(allBinds);

            // add extra requirment based on the image name
            dockerHostConfig = addExceptionalConditions(image, allBinds, dockerHostConfig);

            CreateContainerResponse container = dockerClient.createContainerCmd(image)
                .withName(containerName)
                .withEnv(mapToEnvironmentArray(variables))
                .withHostConfig(dockerHostConfig)
                .exec();

            dockerClient.startContainerCmd(container.getId()).exec();

            return container.getId();
        }else{
            throw new RuntimeException("there is no docker clients exist");
        }
    }

    // If need especial requirement for an image , just  add them here
    private HostConfig addExceptionalConditions(String image, List<Bind> allBinds, HostConfig dockerHostConfig) {
        HostConfig changedDockerHostConfig = dockerHostConfig;
        if(image.contains("enexa-dice-embeddings")){
            // increase the size of shared memory for this module
            LOGGER.info("### increase the shared memory to 32 GB ####");
            changedDockerHostConfig = HostConfig
                .newHostConfig()
                .withNetworkMode(NETWORK_NAME)
                .withBinds(allBinds)
                .withShmSize(32L * 1024 * 1024 * 1024) ; // 32 GB
        }

        if(image.contains("enexa-cel-train-module")){
            // increase the size of shared memory for this module
            LOGGER.info("### increase the shared memory to 8 GB ####");
            changedDockerHostConfig = HostConfig
                .newHostConfig()
                .withNetworkMode(NETWORK_NAME)
                .withBinds(allBinds)
                .withShmSize(8L * 1024 * 1024 * 1024) ; // 8 GB
        }

        return changedDockerHostConfig;
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
            Container c = searchContainerByNameOrId(containerName);
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
    public String getContainerStatus(String containerNameOrId) {
        try{
            Container c = searchContainerByNameOrId(containerNameOrId);
            if(c==null){
                LOGGER.error("there is no container for this Name:"+containerNameOrId);
                return null;
            }
            return c.getState();
        } catch (Exception e) {
            LOGGER.error("Got an exception while trying to get the status of \"" + containerNameOrId + "\". Returning null.", e);
            return null;
        }
    }

    private Container searchContainerByNameOrId(String containerNameOrId){
        List<Container> containers = this.dockerClient.listContainersCmd()
            .withShowAll(true)
            .exec();
        if(containers==null){return null;}
        boolean containerExists = false;
        for (Container container : containers) {
            for(String name :container.getNames()) {
                if (name.contains(containerNameOrId)) {
                    return container;
                }
            }
            if (container.getId().equals(containerNameOrId)) {
                return container;
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
