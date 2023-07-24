package eu.enexa.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

import eu.enexa.service.ContainerManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.List;

@Component("dockerContainerManager")
public class ContainerManagerImpl implements ContainerManager {
    private static final String VOLUME_PATH = "/enexa";

    @Override
    public String startContainer(String image, String containerName, List<AbstractMap.SimpleEntry<String, String>> variables) {
        //DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        //DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

        /*CreateContainerResponse container = dockerClient.createContainerCmd(image)
            .withName(containerName)
            .withEnv(mapToEnvironmentArray(variables))
            .withVolumes(Volume.parse(VOLUME_PATH))
            .withBinds(new Bind("host/path", Volume.parse(VOLUME_PATH)))
            .exec();

        dockerClient.startContainerCmd(container.getId()).exec();*/
        //return container.getId();
        return containerName;
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
