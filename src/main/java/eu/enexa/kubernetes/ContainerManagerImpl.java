package eu.enexa.kubernetes;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.generic.KubernetesApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import eu.enexa.service.ContainerManager;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.generic.GenericKubernetesApi;

@Profile("kubernetes")
@Component("kubernetesContainerManager")
public class ContainerManagerImpl implements ContainerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManagerImpl.class);

    private ApiClient client;

    @Value("${container.manager.namespace:default}")
    private String nameSpace;
    @Value("${container.manager.timeoutMilliSeconds:60000}")
    private int TIMEOUT_MILLISECONDS;


    protected ContainerManagerImpl() {
        try {
            client = initiateClient();
        }catch (Exception ex){
            LOGGER.error(ex.getMessage());
        }
    }

    /**
     * Initializes and configures a Kubernetes API client with custom timeout settings.
     *
     * This method:
     * - Creates a default Kubernetes API client using the default configuration.
     * - Sets custom connection, read, and write timeouts.
     * - Sets the configured client as the default client in the global Configuration class.
     *
     * @return ApiClient configured with specified timeouts.
     * @throws IOException if an error occurs during client initialization.
     */
    protected ApiClient initiateClient() throws IOException {
        try{
            LOGGER.info("initiating Kubernetes client");
            ApiClient client = Config.defaultClient();
            client.setConnectTimeout(TIMEOUT_MILLISECONDS);
            client.setReadTimeout(TIMEOUT_MILLISECONDS);
            client.setWriteTimeout(TIMEOUT_MILLISECONDS);
            Configuration.setDefaultApiClient(client);
            LOGGER.info("Kubernetes API client initiated with default configuration.");
            return client;
        }
        catch (Exception ex){
            LOGGER.error("Failed to initiate Kubernetes API client", ex);
            throw ex;
        }
    }

    protected ContainerManagerImpl(ApiClient client) {
        this.client = client;
    }

    @Override
    public String startContainer(String image, String podName,
            List<AbstractMap.SimpleEntry<String, String>> variables, String hostSharedDirectory, String appName) {
        return startContainerKub(image, podName, variables, hostSharedDirectory, null, appName);
    }

    /**
     * Creates a Kubernetes service of type NodePort for the given pod.
     *
     * @param podUid the unique identifier of the pod
     * @param podPort the port of the pod to expose
     * @param labels the labels for the service selector
     * @return the NodePort assigned to the created service
     * @throws ApiException if there's an error interacting with the Kubernetes API
     */
    public Map<String,String> getServiceAsNodePort(String podUid, Integer podPort ,Map<String,String> labels) throws ApiException {
        LOGGER.info("Creating service as NodePort for pod UID: {}", podUid);
        try {
            if (client == null) {
                LOGGER.error("client is null");
                client = initiateClient();
            }
        }catch (Exception ex){
            LOGGER.error("Error initializing client: ", ex);
        }

        // Create a unique service name using the pod UID and a random UUID
        String name = String.format("ser-%s-%s", podUid, UUID.randomUUID());
        LOGGER.info("Service name is: {}", name);

        // Trim the name if it exceeds 60 characters to make a valid name
        if (name.length() > 60) {
            // add extra S at the end because it could be - and make the name invalid
            name = name.substring(0, 59)+"s";
            LOGGER.info("Service name after trimming: {}", name);
        }

        V1ServicePort serviceport = new V1ServicePortBuilder().withPort(podPort).withTargetPort(new IntOrString(0)).build();
        LOGGER.info("service is :{}", name);
        V1Service service = new V1ServiceBuilder()
            .withNewMetadata().withName(name).endMetadata()
            .withNewSpec()
            .withSelector(labels)
            .withType("NodePort")  // Set service type to NodePort
            .withPorts(serviceport)          // Exposed port
            .endSpec()
            .build();

        LOGGER.info(service.toString());

        // Create Service in Kubernetes
        CoreV1Api api = new CoreV1Api(client);
        LOGGER.info("api client initiated :{}", name);
        //TODO if need check if the service exist !
        try {
            api.createNamespacedService(nameSpace, service, null, null, null, null);
        } catch (NullPointerException e) {
            System.err.println("Caught NullPointerException while creating service: " + e.getMessage());
        } catch (ApiException e) {
            System.err.println("API Exception: " + e.getResponseBody());
        }
        // Retrieve the created service to fetch the NodePort assigned by Kubernetes
        V1Service createdService = api.readNamespacedService(name, nameSpace, null);
        LOGGER.info("Service created with NodePort: {}", createdService.getSpec().getPorts().get(0).getNodePort());

        Map<String,String> serviceSpecs = new HashMap<>();
        serviceSpecs.put("port", Objects.requireNonNull(createdService.getSpec().getPorts().get(0).getNodePort()).toString());
        serviceSpecs.put("externalName",createdService.getSpec().getExternalName());
        serviceSpecs.put("clusterIP",createdService.getSpec().getClusterIP());

        return serviceSpecs;
    }

    /**
     * Starts a container by creating a pod in Kubernetes with the specified configuration.
     *
     * @param image The image to use for the container.
     * @param podName The name of the pod.
     * @param variables Environment variables for the container.
     * @param hostSharedDirectory Path for the host's shared directory.
     * @param command Command to run inside the container.
     * @param appName The application name for directory structure.
     * @return The UID of the created pod, or null if creation failed.
     */
    public String startContainerKub(String image, String podName, List<AbstractMap.SimpleEntry<String, String>> variables,String hostSharedDirectory ,String[] command, String appName) {
        LOGGER.info("Starting container: Image = {}, Pod Name = {}, Variables Size = {}, Host Shared Directory = {}, App Name = {}",
            image, podName, variables.size(), hostSharedDirectory, appName);

        try {
            if (client == null) {
                client = initiateClient();
            }
        }catch (Exception ex){
            LOGGER.error("Error initializing Kubernetes client: ", ex);
            return null;
        }
        // TODO : make this part not hardcoded if there is chance of changing the "urn:container:docker:image:"
        image = image.replace("urn:container:docker:image:","");
        LOGGER.info("Using image: {}", image);

        // Create environment variables
        List<V1EnvVar> env = createEnvVariables(variables);

        // Process experiment IRI variable
        String expIRI=extractExperimentIRI(variables);
        LOGGER.info("ENEXA_EXPERIMENT_IRI: {}", expIRI);

        // Validate IRI
        if(expIRI.length() < 10){
            LOGGER.warn("ENEXA_EXPERIMENT_IRI is null or less than 10 character");
        }

        // Set up paths for shared directories
        String containerBasePath = "/enexa";
        String hostBasePath = buildHostPath(hostSharedDirectory, appName);
        String containerWritablePath = buildWritableContainerPath(expIRI, hostBasePath);
        String containerModuleInstancePath = combinePaths(containerWritablePath, UUID.randomUUID().toString());


        // Add environment variables related to shared directories
        addSharedDirectoryEnvVars(env, containerBasePath, containerModuleInstancePath, containerWritablePath);

        // Set up the persistent volume claim for shared directory
        V1Volume volume = createVolume(hostBasePath);

        // Set up resource requirements
        V1ResourceRequirements resourceRequirements = createResourceRequirements();

        // Create container
        V1Container container = createContainer(image, command, env, resourceRequirements);


        V1VolumeMount volumeMount = new V1VolumeMount();
        volumeMount.setName("enexa-shared-dir");
        volumeMount.setMountPath("/enexa");

        LOGGER.info("mouth path is /enexa");

        container.setVolumeMounts(Arrays.asList(volumeMount));

        // Create a PodSpec and add the shared volume and container
        V1PodSpec podSpec = new V1PodSpec();
        podSpec.restartPolicy("Never");
        // podSpec.setVolumes(Arrays.asList(sharedVolume));
        podSpec.setVolumes(Arrays.asList(volume));
        podSpec.setContainers(Arrays.asList(container));

        V1Pod pod = new V1Pod();
        // Create a Pod
        pod.setMetadata(new V1ObjectMeta().name(podName).namespace(nameSpace).labels(new HashMap<String,String>(){{
                put("app",container.getName());
            }}));
            pod.setSpec(podSpec);
        try {
            GenericKubernetesApi<V1Pod, V1PodList> podClient = new GenericKubernetesApi<>(V1Pod.class, V1PodList.class, "",
                "v1", "pods", client);

            pod.getSpec().getContainers().get(0).setEnv(env);  // add to the first container because we assume runnig one container in each pod

            V1Pod latestPod = podClient.create(pod).throwsApiException().getObject();
            String latestPodUid = latestPod.getMetadata().getUid();
            LOGGER.info("latestPodUID : {}", latestPodUid);
            return latestPodUid;
        } catch (ApiException e) {
            LOGGER.error("Got an exception while trying to create an instance of \"{}\". Returning null.", image, e);
            return null;
        }
    }

    /**
     * Creates environment variables from the provided list of key-value pairs.
     */
    private List<V1EnvVar> createEnvVariables(List<AbstractMap.SimpleEntry<String, String>> variables) {
        List<V1EnvVar> env = new ArrayList<>();
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables) {
                V1EnvVar v1env = new V1EnvVar();
                v1env.setName(entry.getKey());
                v1env.setValue(entry.getValue());
                env.add(v1env);
            }
        }
        return env;
    }

    /**
     * Extracts the experiment IRI from the environment variables list.
     */
    private String extractExperimentIRI(List<AbstractMap.SimpleEntry<String, String>> variables) {
        for (AbstractMap.SimpleEntry<String, String> v : variables) {
            if ("ENEXA_EXPERIMENT_IRI".equals(v.getKey())) {
                return v.getValue();
            }
        }
        return "";
    }


    /**
     * Builds the host path using the shared directory path and app name.
     */
    private String buildHostPath(String hostSharedDirectory, String appName) {
        LOGGER.info("build host path");
        return makeTheDirectoryInThisPath(hostSharedDirectory, appName);
    }

    /**
     * Creates a writable path for the container based on the experiment IRI.
     */
    private String buildWritableContainerPath(String expIRI, String hostBasePath) {
        String writeableDirectory = expIRI.split("/")[expIRI.split("/").length - 1];
        LOGGER.info("build writeable path at {}", writeableDirectory);
        return makeTheDirectoryInThisPath(hostBasePath, writeableDirectory);
    }

    /**
     * Adds environment variables related to shared directories to the list of env variables.
     */
    private void addSharedDirectoryEnvVars(List<V1EnvVar> env, String containerBasePath, String containerModuleInstancePath,
                                           String containerWritablePath) {
        env.add(new V1EnvVar().name("ENEXA_SHARED_DIRECTORY").value(containerBasePath));
        env.add(new V1EnvVar().name("ENEXA_MODULE_INSTANCE_DIRECTORY").value(containerModuleInstancePath));
        env.add(new V1EnvVar().name("ENEXA_WRITEABLE_DIRECTORY").value(containerWritablePath));
    }

    /**
     * Creates a volume for the shared directory.
     */
    private V1Volume createVolume(String hostBasePath) {
        V1PersistentVolumeClaimVolumeSource persistentVolumeClaim = new V1PersistentVolumeClaimVolumeSource();
        persistentVolumeClaim.setClaimName("enexa-shared-dir-claim");

        V1Volume volume = new V1Volume();
        volume.setName("enexa-shared-dir");
        volume.setPersistentVolumeClaim(persistentVolumeClaim);

        return volume;
    }

    /**
     * Creates the resource requirements for the container.
     */
    //TODO 8Gi should be configurable ?
    private V1ResourceRequirements createResourceRequirements() {
        V1ResourceRequirements resourceRequirements = new V1ResourceRequirements();
        Map<String, Quantity> requests = new HashMap<>();
        requests.put("memory", new Quantity("8Gi"));
        resourceRequirements.setRequests(requests);
        return resourceRequirements;
    }

    /**
     * Creates a container with the given image, command, environment variables, and resource requirements.
     */
    private V1Container createContainer(String image, String[] command, List<V1EnvVar> env, V1ResourceRequirements resourceRequirements) {
        V1Container container = new V1Container();
        String containerName = image.replace("/", "-").replace(".", "").replace(":", "").toLowerCase();
        container.setName(containerName);
        container.setImage(image);
        container.setResources(resourceRequirements);

        if (command != null) {
            container.setCommand(Arrays.asList(command));
        }

        container.setEnv(env);
        return container;
    }

    @Override
    public String stopContainer(String containerId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getContainerStatus(String podName) {
        LOGGER.info("client is base path:{} timeout : {}", client.getBasePath(), client.getConnectTimeout());
        GenericKubernetesApi<V1Pod, V1PodList> podClient = new GenericKubernetesApi<>(
            V1Pod.class, V1PodList.class, "", "v1", "pods", client);
        LOGGER.info("GenericKubernetesApi for Pods initiated ");
        try {
            // List all pods in the specified namespace
            KubernetesApiResponse<V1PodList> response = podClient.list(nameSpace);
            if (response.isSuccess()) {
                V1PodList podList = response.getObject();
                LOGGER.info("the list of pods size is {}", podList.getItems().size());
                LOGGER.info("looking for {}", podName);
                for (V1Pod pod : podList.getItems()) {
                    LOGGER.info("podName : {}", pod.getMetadata().getName());
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
public Map<String, String> resolveContainerEndpoint(String containerId, Integer port){
        LOGGER.info("start resolving the IP from ID: {}",containerId);
    Map<String, String> endpointMap = new HashMap<>();

    GenericKubernetesApi<V1Pod, V1PodList> podClient = new GenericKubernetesApi<>(
        V1Pod.class, V1PodList.class, "", "v1", "pods", client);


try {
    // List all pods (you can filter by namespace if needed, e.g., "default" namespace)
    V1PodList podList = podClient.list(nameSpace).getObject();
    if (podList == null || podList.getItems().isEmpty()) {
        LOGGER.error("No pods found in the '{}' namespace.",nameSpace);
        return null;
    }
    LOGGER.info("Number of pods retrieved: {}", podList.getItems().size());

    V1Pod targetPod = podList.getItems().stream()
        .filter(pod -> containerId.equals(pod.getMetadata().getUid()))
        .findFirst()
        .orElse(null);

    if (targetPod == null) {
        LOGGER.warn("No pod found with UID: {}", containerId);
        return null;
    }
    LOGGER.info("Target pod found: {}", targetPod.getMetadata().getName());

// Wait for the pod to start running (with a timeout of 60 seconds)
    long startTime = System.currentTimeMillis();
    while (true) {
        LOGGER.info("checking the status of the pod");
        // Check if the pod is in the 'Running' state
        String podPhase = targetPod.getStatus().getPhase();
        LOGGER.info("podPhase : {}", podPhase);
        if ("Running".equals(podPhase)) {
            // Create the service as a Nodeport
            // get new port
            // make complete endpoint andk return it
            LOGGER.info("pod is running");
            Map<String, String> pod_labels = targetPod.getMetadata().getLabels();
            if(pod_labels==null){
                LOGGER.error("there is no label in a pod to make selector ! podname is "+containerId);
            }else{
                LOGGER.info("pod_labels size: " + pod_labels.size());
            }
            String podIp = targetPod.getStatus().getPodIP();
            String HostIP = targetPod.getStatus().getHostIP();
            LOGGER.info("status is {}",targetPod.getStatus().toString());
            Map<String,String> serviceSpecs = getServiceAsNodePort(containerId,port,pod_labels);

            if(serviceSpecs==null){
                LOGGER.error("could not get a port for this");
             return null;
            }else {
                String serviceSpecsMapAsString = serviceSpecs.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining(", "));

                LOGGER.info(serviceSpecsMapAsString);

                endpointMap.put("internalEndpointURL",podIp + ":" + port);
                endpointMap.put("hostIP",HostIP);
                endpointMap.put("externalEndpointURL",serviceSpecs.get("clusterIP") + ":" + serviceSpecs.get("port"));

                String endpointMapAsString = endpointMap.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining(", "));

                LOGGER.info(endpointMapAsString);

                return endpointMap;
            }
        }


        if (System.currentTimeMillis() - startTime > 180 * 1000) {
            LOGGER.error("Pod with UID {} did not start running within 180 seconds", containerId);
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
    LOGGER.error("Failed to resolve endpoint{}", ex.getMessage());
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
