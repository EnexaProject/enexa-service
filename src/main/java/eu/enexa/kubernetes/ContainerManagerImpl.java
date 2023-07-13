package eu.enexa.kubernetes;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.enexa.service.ContainerManager;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import org.springframework.stereotype.Component;

@Component
public class ContainerManagerImpl implements ContainerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManagerImpl.class);

    private ApiClient client;

    protected ContainerManagerImpl(){

    }

    protected ContainerManagerImpl(ApiClient client) {
        this.client = client;
    }

    @Override
    public String startContainer(String image, String podName) {

        //TODO : maybe need change container name "b" to variable
        V1Pod pod = new V1Pod().metadata(new V1ObjectMeta().name(podName).namespace("default")).spec(new V1PodSpec()
                .restartPolicy("Never").containers(Arrays.asList(new V1Container().name("b").image(image))));

        GenericKubernetesApi<V1Pod, V1PodList> podClient = new GenericKubernetesApi<>(V1Pod.class, V1PodList.class, "",
                "v1", "pods", client);

        try {
            V1Pod latestPod = podClient.create(pod).throwsApiException().getObject();
            return latestPod.getMetadata().getName();
        } catch (ApiException e) {
            LOGGER.error("Got an exception while trying to create an instance of \"" + image + "\". Returning null.",
                    e);
            return null;
        }
    }

    @Override
    public String stopContainer(String containerId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getContainerStatus(String containerId) {
        // TODO Auto-generated method stub
        return null;
    }

    public static ContainerManagerImpl create() throws IOException {
            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);
            return new ContainerManagerImpl(client);
    }

    public static void main(String[] args) throws IOException {
        ContainerManagerImpl manager = ContainerManagerImpl.create();
        System.out.println(manager.startContainer("busybox","test"+ UUID.randomUUID().toString()));
    }
}
