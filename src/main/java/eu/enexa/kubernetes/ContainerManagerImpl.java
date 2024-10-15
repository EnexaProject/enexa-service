package eu.enexa.kubernetes;

import java.io.File;
import java.io.IOException;
import java.util.*;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.generic.KubernetesApiResponse;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import eu.enexa.service.ContainerManager;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.generic.GenericKubernetesApi;

import static org.apache.jena.atlas.lib.RandomLib.random;

@Primary
@Component("kubernetesContainerManager")
public class ContainerManagerImpl implements ContainerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManagerImpl.class);

    private ApiClient client;

    private String nameSpace = "default";

    protected ContainerManagerImpl() {
        try {
            client = initiateClient();
        }catch (Exception ex){
            LOGGER.error(ex.getMessage());
        }
    }

    protected ApiClient initiateClient() throws IOException {
        //Config.defaultClient();
        //ClientBuilder.standard().build();
        LOGGER.info("initiating Kubernetes client");
        ApiClient client = Config.defaultClient();
        client.setConnectTimeout(60000); // 60 seconds
        client.setReadTimeout(60000);
        client.setWriteTimeout(60000);
        Configuration.setDefaultApiClient(client);
        LOGGER.info("Kubernetes API client initiated with default configuration.");
        return client;
    }

    protected ContainerManagerImpl(ApiClient client) {
        this.client = client;
    }

    @Override
    public String startContainer(String image, String podName,
            List<AbstractMap.SimpleEntry<String, String>> variables, String hostSharedDirectory, String appName) {
        return startContainerKub(image, podName, variables, hostSharedDirectory, null, appName);
    }

    // podName is the containerName for kubernetes
    public String startContainerKub(String image, String podName, List<AbstractMap.SimpleEntry<String, String>> variables,String hostSharedDirectory ,String[] command, String appName) {
        LOGGER.info("Starting a container image is "+ image+" podName is :"+ podName+" variable size is : "+ variables.size()+" hostSharedDirectory : "+hostSharedDirectory+" appName is :"+appName);
        try {
            if (client == null) {
                client = initiateClient();
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        // TODO : make this part not hardcoded if there is chance of changing the "urn:container:docker:image:"
        image = image.replace("urn:container:docker:image:","");
        LOGGER.info("image name is : "+ image);

        List<V1EnvVar> env = new ArrayList<>();
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables) {
                V1EnvVar v1env = new V1EnvVar();
                v1env.setName(entry.getKey());
                v1env.setValue(entry.getValue());
                env.add(v1env);
            }
        }

        String expIRI="";

        for(AbstractMap.SimpleEntry v:variables){
            if(v.getKey().toString().equals("ENEXA_EXPERIMENT_IRI")){
                expIRI = v.getValue().toString();
            }
        }

        LOGGER.info("ENEXA_EXPERIMENT_IRI is : "+expIRI);

        if(expIRI.equals("")||expIRI.length()<10){
            LOGGER.warn("ENEXA_EXPERIMENT_IRI is null or less than 10 character");
        }

        String containerSharedDirectory = "/enexa";
        String containerBasePath = makeTheDirectoryInThisPath(containerSharedDirectory,appName);
        String hostBasePath = makeTheDirectoryInThisPath(hostSharedDirectory,appName);

        String writeableDirectory =expIRI.split("/")[expIRI.split("/").length - 1];
        String hostWritablePath = makeTheDirectoryInThisPath(hostBasePath,writeableDirectory);
        String containerWritablePath = combinePaths(containerBasePath,writeableDirectory);
        String moduleInstanceIRI = UUID.randomUUID().toString();
        //String hostModuleInstancePath = makeTheDirectoryInThisPath(hostWritablePath,moduleInstanceIRI);
        String containerModuleInstancePath = combinePaths(containerWritablePath, moduleInstanceIRI);

        env.add(new V1EnvVar()
            .name("ENEXA_SHARED_DIRECTORY")
            .value(containerSharedDirectory));
        LOGGER.info("ENEXA_SHARED_DIRECTORY :" + containerSharedDirectory);

        env.add(new V1EnvVar()
            .name("ENEXA_MODULE_INSTANCE_DIRECTORY")
            .value(containerModuleInstancePath));
        LOGGER.info("ENEXA_MODULE_INSTANCE_DIRECTORY :" + containerModuleInstancePath);

        env.add(new V1EnvVar()
            .name("ENEXA_WRITEABLE_DIRECTORY")
            .value(containerWritablePath));
        LOGGER.info("ENEXA_WRITEABLE_DIRECTORY :" + containerWritablePath);

        /*
         * // Create a shared volume V1Volume sharedVolume = new V1Volume();
         * sharedVolume.setName("shared-volume");
         *
         * // Define the shared volume source V1EmptyDirVolumeSource
         * emptyDirVolumeSource = new V1EmptyDirVolumeSource();
         * sharedVolume.setEmptyDir(emptyDirVolumeSource);
         */

        // Create a VolumeClaim
        V1PersistentVolumeClaimVolumeSource persistentVolumeClaim = new V1PersistentVolumeClaimVolumeSource();
        persistentVolumeClaim.setClaimName("enexa-shared-dir-claim");


        V1Volume volume = new V1Volume();
        volume.setName("enexa-shared-dir");
        //volume.setHostPath(new V1HostPathVolumeSource().path(hostBasePath));
        volume.setPersistentVolumeClaim(persistentVolumeClaim);

        LOGGER.info("hostBasePath : "+hostBasePath);

        // Set resource limits
        V1ResourceRequirements resourceRequirements = new V1ResourceRequirements();
//        Map<String, Quantity> limits = new HashMap<>();
//        limits.put("memory", new Quantity("28Gi"));
//        resourceRequirements.setLimits(limits);

        Map<String, Quantity> requests = new HashMap<>();
        requests.put("memory", new Quantity("8Gi"));
        resourceRequirements.setRequests(requests);


        // Create a container and set the volume mount
        V1Container container = new V1Container();
        String containerName = image.replace("/","-").replace(".","").replace(":","").toLowerCase();
        LOGGER.info("containerName : "+containerName);
        container.setName(containerName);
        container.setImage(image);
        //container.setImage("hub.cs.upb.de/enexa/images/enexa-extraction-module:1.0.0");
        container.setResources(resourceRequirements);
        if (command != null) {
            for (int i = 0; i < command.length; ++i) {
                container.addCommandItem(command[i]);
            }
        }

        V1VolumeMount volumeMount = new V1VolumeMount();
        volumeMount.setName("enexa-shared-dir");
        volumeMount.setMountPath("/enexa");

        LOGGER.info("mounth path is /enexa");

        container.setVolumeMounts(Arrays.asList(volumeMount));

        // Create a PodSpec and add the shared volume and container
        V1PodSpec podSpec = new V1PodSpec();
        podSpec.restartPolicy("Never");
        // podSpec.setVolumes(Arrays.asList(sharedVolume));
        podSpec.setVolumes(Arrays.asList(volume));
        podSpec.setContainers(Arrays.asList(container));
        V1Pod pod = new V1Pod();
        // Create a Pod with the PodSpec
        pod.setMetadata(new V1ObjectMeta().name(podName).namespace(nameSpace).labels(new HashMap<String,String>(){{
                put("app",containerName);
            }}));
            pod.setSpec(podSpec);


        try {

            GenericKubernetesApi<V1Pod, V1PodList> podClient = new GenericKubernetesApi<>(V1Pod.class, V1PodList.class, "",
                "v1", "pods", client);


            pod.getSpec().getContainers().get(0).setEnv(env);  // add to the first container because we assume runnig one container in each pod

            V1Pod latestPod = podClient.create(pod).throwsApiException().getObject();
            String latestPodUid = latestPod.getMetadata().getUid();
            LOGGER.info("latestPodUID : "+latestPodUid);
            return latestPodUid;
        } catch (ApiException e) {
            e.printStackTrace();
            LOGGER.error("Got an exception while trying to create an instance of \"" + image + "\". Returning null.",
                    e);
            return null;
        }
    }
    //TODO : move following two method in a utility class shared between docker and kubernetes
    /**
     * Combines two path components to create a valid path.
     * the directory will create if not exist
     *
     * @param partOneOfPath   The first part of the path.
     * @param partTwoOfPath   The second part of the path.
     * @return                The combined path.
     */
    private String makeTheDirectoryInThisPath(String partOneOfPath, String partTwoOfPath) {
        if(partOneOfPath ==null && partTwoOfPath == null) return "";
        String path = combinePaths(partOneOfPath, partTwoOfPath);
        File appPathDirectory = new File(path);
        if(!appPathDirectory.exists()){
            appPathDirectory.mkdirs();
        }
        return path;
    }

    /**
     * Combines two path components to create a valid path, taking into account trailing separators.
     *
     * @param partOne   The first part of the path.
     * @param partTwo   The second part of the path.
     * @return          The combined path.
     */
    public static String combinePaths(String partOne, String partTwo) {
        String path = partOne + File.separator + partTwo;
        if (partOne.endsWith(File.separator)) {
            path = partOne + partTwo;
        }
        return path;
    }

    @Override
    public String stopContainer(String containerId) {
        // TODO Auto-generated method stub
        return null;
    }

//    @Override
//    public String getContainerStatus(String podName) {
//        LOGGER.info("client is base path:"+client.getBasePath()+" timeout : "+client.getConnectTimeout());
//        CoreV1Api api = new CoreV1Api(client);
//        LOGGER.info(" CoreV1Api initiated ");
//        try {
//            V1PodList list = api.listNamespacedPod(nameSpace, "false", null, null, null, null, null, null, null, null,
//                    null);
//            LOGGER.info("the list of pods size is "+list.getItems().size());
//            LOGGER.info("looking for "+podName);
//            for (V1Pod pod : list.getItems()) {
//                LOGGER.info("podName : "+pod.getMetadata().getName());
//                if (pod.getMetadata().getName().equals(podName)) {
//                    return pod.getStatus().getPhase();
//                }
//            }
//        } catch (ApiException e) {
//            LOGGER.error("Got an exception while trying to get the status of \"" + podName + "\". Returning null.", e);
//            LOGGER.error(e.getMessage());
//            e.printStackTrace();
//            return null;
//        }
//        return null;
//    }

    @Override
    public String getContainerStatus(String podName) {
        LOGGER.info("client is base path:" + client.getBasePath() + " timeout : " + client.getConnectTimeout());
        GenericKubernetesApi<V1Pod, V1PodList> podClient = new GenericKubernetesApi<>(
            V1Pod.class, V1PodList.class, "", "v1", "pods", client);
        LOGGER.info("GenericKubernetesApi for Pods initiated ");
        try {
            // List all pods in the specified namespace
            KubernetesApiResponse<V1PodList> response = podClient.list(nameSpace);
            if (response.isSuccess()) {
                V1PodList podList = response.getObject();
                LOGGER.info("the list of pods size is " + podList.getItems().size());
                LOGGER.info("looking for " + podName);
                for (V1Pod pod : podList.getItems()) {
                    LOGGER.info("podName : " + pod.getMetadata().getName());
                    if (pod.getMetadata().getName().equals(podName)) {
                        return pod.getStatus().getPhase();
                    }
                }
            } else {
                LOGGER.error("Failed to list pods. Status code: " + response.getHttpStatusCode());
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("Got an exception while trying to get the status of \"" + podName + "\". Returning null.", e);
            return null;
        }
        return null;
    }


@Override
public String resolveContainerEndpoint(String containerId){
        LOGGER.info("start resolving the IP from ID" +containerId);
    GenericKubernetesApi<V1Pod, V1PodList> podClient = new GenericKubernetesApi<>(
        V1Pod.class, V1PodList.class, "", "v1", "pods", client);
try {
    // List all pods (you can filter by namespace if needed, e.g., "default" namespace)
    V1PodList podList = podClient.list("default").getObject();
    LOGGER.info("the list of all pods size is " + podList.getItems().size());
    V1Pod targetPod = null;
    // Iterate over the pods to find the one with the matching UID
    for (V1Pod pod : podList.getItems()) {
        LOGGER.info("podName : " + pod.getMetadata().getName() + " podUID"+ pod.getMetadata().getUid()+" POD IP :"+pod.getStatus().getPodIP());
        if (pod.getMetadata().getUid().equals(containerId)) {
            targetPod = pod;
            LOGGER.info("POD is loaded");
            break;
        }
    }

    if (targetPod == null) {
        return null;
    }

// Wait for the pod to start running (with a timeout of 60 seconds)
    long startTime = System.currentTimeMillis();
    while (true) {
        // Check if the pod is in the 'Running' state
        String podPhase = targetPod.getStatus().getPhase();
        if ("Running".equals(podPhase)) {
            // Return the Pod IP once it's running
            return targetPod.getStatus().getPodIP();
        }


        if (System.currentTimeMillis() - startTime > 120 * 1000) {
            LOGGER.error("Pod with UID " + containerId + " did not start running within 60 seconds");
            return null;
        }

        // Sleep for a short interval before checking again
        Thread.sleep(5000); // Sleep for 2 seconds

        // Re-fetch the pod to update its status
        targetPod = podClient.get("default", targetPod.getMetadata().getName())
            .throwsApiException()
            .getObject();
    }

}catch (Exception ex){
    LOGGER.error("Failed to resolve endpoint" + ex.getMessage());
    return null;
}
}

    public static ContainerManagerImpl create() throws IOException {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        return new ContainerManagerImpl(client);
    }

    public static void main(String[] args) throws Exception {
        ContainerManagerImpl manager = ContainerManagerImpl.create();
        String containerId = manager.startContainerKub("busybox", "test" + UUID.randomUUID().toString(), null, null,new String[] { "sleep", "10000" },null);
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
