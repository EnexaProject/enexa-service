package eu.enexa.docker;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.httpclient5.*;


import com.github.dockerjava.core.DockerClientImpl;
import eu.enexa.service.ContainerManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.List;

//TODO: remove primary after using config for beans
@Primary
@Component("dockerContainerManager")
public class ContainerManagerImpl implements ContainerManager {
    private static final String VOLUME_PATH = "/enexa";

    @Override
    public String startContainer(String image, String containerName, List<AbstractMap.SimpleEntry<String, String>> variables) {
        DockerClientConfig standard = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(standard.getDockerHost())
            .sslConfig(standard.getSSLConfig())
            .build();

        DockerClient dockerClient = DockerClientImpl.getInstance(standard, httpClient);

        // when could not pinging then the exception will be raised
        dockerClient.pingCmd().exec();

        CreateContainerResponse container = dockerClient.createContainerCmd(image)
            .withName(containerName)
            .withEnv(mapToEnvironmentArray(variables))
            .withVolumes(Volume.parse(VOLUME_PATH))
            .exec();

        dockerClient.startContainerCmd(container.getId()).exec();

        return container.getId();
    }

    private String[] mapToEnvironmentArray(List<AbstractMap.SimpleEntry<String, String>> environmentVariables) {
        if (environmentVariables == null || environmentVariables.isEmpty()) {
            return null;
        }

        return environmentVariables.stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .toArray(String[]::new);
    }

    @Override
    public String stopContainer(String podName) {
        return null;
    }

    @Override
    public String getContainerStatus(String podName) {
        return null;
    }
}
