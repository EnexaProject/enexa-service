package eu.enexa.kubernetes;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.kubernetes.client.openapi.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import eu.enexa.service.ContainerManager;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.generic.GenericKubernetesApi;

@Component
public class ContainerManagerImpl implements ContainerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManagerImpl.class);

    private ApiClient client;

    protected ContainerManagerImpl() {

    }

    protected ContainerManagerImpl(ApiClient client) {
        this.client = client;
    }

    @Override
    public String startContainer(String image, String podName,
            List<AbstractMap.SimpleEntry<String, String>> variables) {

        List<V1EnvVar> env = new ArrayList<>();
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables) {
                V1EnvVar v1env = new V1EnvVar();
                v1env.setName(entry.getKey());
                v1env.setValue(entry.getValue());
                env.add(v1env);
            }
        }

        // TODO : maybe need change container name "b" to variable
        V1Pod pod = new V1Pod().metadata(new V1ObjectMeta().name(podName).namespace("default")).spec(new V1PodSpec()
                .restartPolicy("Never").containers(Arrays.asList(new V1Container().name("b").image(image).env(env))));
//        V1Pod pod = new V1Pod().metadata(new V1ObjectMeta().name(podName).namespace("default")).spec(new V1PodSpec()
//                .restartPolicy("Never").containers(Arrays.asList(new V1Container().name("b").image(image).env(env).addCommandItem("sleep").addCommandItem("20"))));

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
    public String getContainerStatus(String podName) {
        CoreV1Api api = new CoreV1Api(client);
        try {
            V1PodList list = api.listNamespacedPod("default", "false", null, null, null, null, null, null, null, null,
                    null);
            for (V1Pod pod : list.getItems()) {
                if (pod.getMetadata().getName().equals(podName)) {
                    return pod.getStatus().getPhase();
                }
            }
        } catch (ApiException e) {
            LOGGER.error("Got an exception while trying to get the satus of \"" + podName + "\". Returning null.", e);
            return null;
        }
        return null;
    }

    public static ContainerManagerImpl create() throws IOException {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        return new ContainerManagerImpl(client);
    }

    public static void main(String[] args) throws Exception {
        ContainerManagerImpl manager = ContainerManagerImpl.create();
        String containerId = manager.startContainer("busybox", "test" + UUID.randomUUID().toString(), null);
        System.out.println(containerId);

        String status = null;
        while (true) {
            status = manager.getContainerStatus(containerId);
            System.out.println("Status: " + status);
            if (status.equals("Running") || status.equals("Pending")) {
                Thread.sleep(1000);
            } else {
                return;
            }
        }
    }
}
