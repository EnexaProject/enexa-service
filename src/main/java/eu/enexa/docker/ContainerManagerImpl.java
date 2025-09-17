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

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

/**
 * Manages Docker containers, providing functionality to start, stop, and get
 * the status of containers.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
@Profile("docker")
@Component("dockerContainerManager")
public class ContainerManagerImpl implements ContainerManager {

    /**
     * Logger for ContainerManagerImpl class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManagerImpl.class);

    /**
     * Name of the Docker network to which containers are attached.
     */
    private static final String NETWORK_NAME = System.getenv("DOCKER_NET_NAME");

    /**
     * Docker client used for interacting with the Docker daemon.
     */
    private final DockerClient dockerClient;

    @Value("${docker.traefik.hostname}")
    private String hostName;

    @Value("${docker.traefik.loadbalancer.port}")
    private String traefikLoadBalancerPort;

    /**
     * Constructs a new ContainerManagerImpl and initializes the Docker client.
     */
    public ContainerManagerImpl() {
        LOGGER.info("start initiating the ContainerManagerImpl");
        DockerClientConfig standard = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        LOGGER.info("standards are defined");
        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder().dockerHost(standard.getDockerHost())
                .sslConfig(standard.getSSLConfig()).build();
        LOGGER.info("http client is ready");
        this.dockerClient = DockerClientImpl.getInstance(standard, httpClient);
        LOGGER.info("docker client is ready");

        // when could not pinging then the exception will be raised
        try {
            this.dockerClient.pingCmd().exec();
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.info("docker host uri:" + standard.getDockerHost().toString());
            LOGGER.info("API version:" + standard.getApiVersion().toString());
            LOGGER.info("standard.getSSLConfig()" + standard.getSSLConfig().toString());
        }
        LOGGER.info("docker client is pinged");
    }

    @Override
    public String startContainer(String image, String containerName,
            List<AbstractMap.SimpleEntry<String, String>> variables, String hostSharedDirectory, String appName,
            Map<String, String> containerSettings) {
        if (variables == null) {
            variables = new ArrayList<>();
        }
        if (this.dockerClient != null) {
            image = trimImageName(image);

            String expIRI = "";

            for (AbstractMap.SimpleEntry v : variables) {
                if (v.getKey().toString().equals("ENEXA_EXPERIMENT_IRI")) {
                    expIRI = v.getValue().toString();
                }
            }
            if (expIRI.length() < 10) {
                LOGGER.warn("ENEXA_EXPERIMENT_IRI is null or less than 10 character");
            }

            // this hardcoded path should convert to environment variable if used somewhere
            // else
            String containerSharedDirectory = "/home/shared";
            String containerBasePath = makeTheDirectoryInThisPath(containerSharedDirectory, appName);
            String hostBasePath = makeTheDirectoryInThisPath(hostSharedDirectory, appName);

            String writeableDirectory = expIRI.split("/")[expIRI.split("/").length - 1];
            String hostWritablePath = makeTheDirectoryInThisPath(hostBasePath, writeableDirectory);
            String containerWritablePath = combinePaths(containerBasePath, writeableDirectory);
            String moduleInstanceIRI = UUID.randomUUID().toString();
            // String hostModuleInstancePath =
            // makeTheDirectoryInThisPath(hostWritablePath,moduleInstanceIRI);
            String containerModuleInstancePath = combinePaths(containerWritablePath, moduleInstanceIRI);

            variables.add(new AbstractMap.SimpleEntry<>("ENEXA_SHARED_DIRECTORY", containerSharedDirectory));
            variables
                    .add(new AbstractMap.SimpleEntry<>("ENEXA_MODULE_INSTANCE_DIRECTORY", containerModuleInstancePath));
            variables.add(new AbstractMap.SimpleEntry<>("ENEXA_WRITEABLE_DIRECTORY", containerWritablePath));
            List<Bind> allBinds = new ArrayList<>();

            // until Module instance is the subdirectory of writeable directory , we do not
            // need this
            // allBinds.add(new Bind(hostModuleInstancePath, new
            // Volume(containerModuleInstancePath),AccessMode.rw));

            // Order is important here , If add something check it

            allBinds.add(new Bind(hostWritablePath, new Volume(containerWritablePath), AccessMode.rw));
            allBinds.add(new Bind(hostSharedDirectory, new Volume(containerSharedDirectory), AccessMode.ro));

            LOGGER.info("this path from host :{} mapped to this path in container{}", hostSharedDirectory,
                    containerBasePath);
            LOGGER.info("this path from host :{} mapped to this path in container {}", hostWritablePath,
                    containerWritablePath);
            // LOGGER.info("this path from host :"+hostModuleInstancePath+" mapped to this
            // path in container "+containerModuleInstancePath);

            HostConfig dockerHostConfig = HostConfig.newHostConfig().withNetworkMode(NETWORK_NAME).withBinds(allBinds);

            // add extra requirement based on the image name
            dockerHostConfig = addExceptionalConditions(image, allBinds, dockerHostConfig);

            Map<String, String> labels = new HashMap<>();
            labels.put("traefik.enable", "true");

            labels.put("traefik.http.routers." + containerName + ".rule",
                    "Host(\"" + hostName + "\") && PathPrefix(\"/" + containerName + "\")");
            labels.put("traefik.http.routers." + containerName + ".service", containerName);
            labels.put("traefik.http.routers." + containerName + ".entrypoints", "web");

            labels.put("traefik.http.middlewares." + containerName + "-prefix.stripprefix.prefixes",
                    "/" + containerName);
            labels.put("traefik.http.middlewares." + containerName + "-prefix.stripprefix.forceSlash", "true");

            labels.put("traefik.http.routers." + containerName + ".middlewares", containerName + "-prefix");

//            LOGGER.info("traefik.http.routers."+containerName+".entrypoints:web");

            labels.put("traefik.http.services." + containerName + ".loadbalancer.server.port", traefikLoadBalancerPort);
            LOGGER.info(
                    "traefik.http.services." + containerName + ".loadbalancer.server.port:" + traefikLoadBalancerPort);

            CreateContainerResponse container = dockerClient.createContainerCmd(image).withName(containerName)
                    .withEnv(mapToEnvironmentArray(variables)).withHostConfig(dockerHostConfig).withLabels(labels)
                    .exec();

            dockerClient.startContainerCmd(container.getId()).exec();

            return container.getId();
        } else {
            throw new RuntimeException("there is no docker clients exist");
        }
    }

    /**
     * Adds exceptional conditions to the Docker host configuration based on the
     * specified Docker image. If need especial requirement for an image , just add
     * them here
     *
     * @param image            The Docker image.
     * @param allBinds         List of binds for shared directories.
     * @param dockerHostConfig The original Docker host configuration.
     * @return The modified Docker host configuration.
     */
    private HostConfig addExceptionalConditions(String image, List<Bind> allBinds, HostConfig dockerHostConfig) {
        HostConfig changedDockerHostConfig = dockerHostConfig;
        if (image.contains("enexa-dice-embeddings")) {
            // increase the size of shared memory for this module
            LOGGER.info("### increase the shared memory to 32 GB ####");
            changedDockerHostConfig = HostConfig.newHostConfig().withNetworkMode(NETWORK_NAME).withBinds(allBinds)
                    .withShmSize(32L * 1024 * 1024 * 1024); // 32 GB
        }

        if (image.contains("enexa-cel-train-module")) {
            // increase the size of shared memory for this module
            LOGGER.info("### increase the shared memory to 8 GB ####");
            changedDockerHostConfig = HostConfig.newHostConfig().withNetworkMode(NETWORK_NAME).withBinds(allBinds)
                    .withShmSize(8L * 1024 * 1024 * 1024); // 8 GB
        }

        return changedDockerHostConfig;
    }

    /**
     * Trims the Docker image name to remove the "docker:image:" prefix.
     *
     * @param image The Docker image name.
     * @return The trimmed image name.
     */
    private String trimImageName(String image) {
        String[] parts = image.split("docker:image:");
        if (parts.length < 2) {
            return image;
        }
        return parts[1];
    }

    /**
     * Converts a list of environment variable entries into an array of strings.
     *
     * @param environmentVariables List of environment variable entries.
     * @return Array of environment variable strings.
     */
    private String[] mapToEnvironmentArray(List<AbstractMap.SimpleEntry<String, String>> environmentVariables) {
        if (environmentVariables == null || environmentVariables.isEmpty()) {
            return new String[0];
        }

        return environmentVariables.stream().map(entry -> entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);
    }

    @Override
    public String stopContainer(String containerName) {
        try {
            Container c = searchContainerByNameOrId(containerName);
            if (c == null) {
                return null;
            }
            dockerClient.stopContainerCmd(c.getId()).exec();
            return containerName;
        } catch (DockerException e) {
            return "Error stopping container with Name " + containerName + ": " + e.getMessage();
        }
    }

    @Override
    public String getContainerStatus(String containerNameOrId) {
        try {
            Container c = searchContainerByNameOrId(containerNameOrId);
            if (c == null) {
                LOGGER.error("there is no container for this Name:" + containerNameOrId);
                return null;
            }
            return c.getState();
        } catch (Exception e) {
            LOGGER.error(
                    "Got an exception while trying to get the status of \"" + containerNameOrId + "\". Returning null.",
                    e);
            return null;
        }
    }

    @Override
    public Map<String, String> resolveContainerEndpoint(String containerId, Integer port) {
        // todo implement this for docker
        // get container name and use as url
        return new HashMap<>();
    }

    /**
     * Searches for a Docker container by name or ID and returns the corresponding
     * Container object.
     *
     * @param containerNameOrId The name or ID of the container.
     * @return The Container object representing the container.
     */
    private Container searchContainerByNameOrId(String containerNameOrId) {
        List<Container> containers = this.dockerClient.listContainersCmd().withShowAll(true).exec();
        if (containers == null) {
            return null;
        }
        for (Container container : containers) {
            for (String name : container.getNames()) {
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
}
