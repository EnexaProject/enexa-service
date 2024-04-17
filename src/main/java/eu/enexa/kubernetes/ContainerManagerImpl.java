package eu.enexa.kubernetes;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.ClientBuilder;
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
            client = Config.defaultClient();
        }catch (Exception ex){
            LOGGER.error(ex.getMessage());
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

    // podName is the containerName for kubernetes
    public String startContainerKub(String image, String podName, List<AbstractMap.SimpleEntry<String, String>> variables,String hostSharedDirectory ,String[] command, String appName) {
        LOGGER.info("Starting a container image is "+ image+" podName is :"+ podName+" variable size is : "+ variables.size()+" hostSharedDirectory : "+hostSharedDirectory+" appName is :"+appName);
        try {
            if (client == null) {
                client = ClientBuilder.standard().build();
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

        String containerSharedDirectory = "/home/shared";
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
        /*V1PersistentVolumeClaimVolumeSource persistentVolumeClaim = new V1PersistentVolumeClaimVolumeSource();
        persistentVolumeClaim.setClaimName("enexa-shared-dir-claim");*/


        V1Volume volume = new V1Volume();
        volume.setName("enexa-volume");
        volume.setHostPath(new V1HostPathVolumeSource().path(hostBasePath));
        //volume.setPersistentVolumeClaim(persistentVolumeClaim);

        LOGGER.info("hostBasePath : "+hostBasePath);

        // Create a container and set the volume mount
        V1Container container = new V1Container();
        String containerName = image.replace("/","-").replace(".","").replace(":","");
        LOGGER.info("containerName : "+containerName);
        container.setName(containerName);
        container.setImage(image);
        //container.setImage("hub.cs.upb.de/enexa/images/enexa-extraction-module:1.0.0");

        if (command != null) {
            for (int i = 0; i < command.length; ++i) {
                container.addCommandItem(command[i]);
            }
        }

        V1VolumeMount volumeMount = new V1VolumeMount();
        volumeMount.setName("enexa-volume");
        volumeMount.setMountPath("/home/shared");

        LOGGER.info("mounth path is /home/shared");

        container.setVolumeMounts(Arrays.asList(volumeMount));

        // Create a PodSpec and add the shared volume and container
        V1PodSpec podSpec = new V1PodSpec();
        podSpec.restartPolicy("Never");
        // podSpec.setVolumes(Arrays.asList(sharedVolume));
        podSpec.setVolumes(Arrays.asList(volume));
        podSpec.setContainers(Arrays.asList(container));

        // Create a Pod with the PodSpec
        V1Pod pod = new V1Pod();
        pod.setMetadata(new V1ObjectMeta().name(podName).namespace(nameSpace));
        pod.setSpec(podSpec);

        // TODO : maybe need change container name "b" to variable
        /*
         * V1Pod pod = new V1Pod().metadata(new
         * V1ObjectMeta().name(podName).namespace("default")).spec(new V1PodSpec()
         * .restartPolicy("Never") .containers(Arrays.asList(new
         * V1Container().name("b").image(image).env(env).setVolumeMounts(Arrays.asList(
         * volumeMount)))));
         */

//        V1Pod pod = new V1Pod().metadata(new V1ObjectMeta().name(podName).namespace("default")).spec(new V1PodSpec()
//                .restartPolicy("Never").containers(Arrays.asList(new V1Container().name("b").image(image).env(env).addCommandItem("sleep").addCommandItem("20"))));



        try {

            GenericKubernetesApi<V1Pod, V1PodList> podClient = new GenericKubernetesApi<>(V1Pod.class, V1PodList.class, "",
                "v1", "pods", client);


            pod.getSpec().getContainers().get(0).setEnv(env);  // add to the first container because we assume runnig one container in each pod

            V1Pod latestPod = podClient.create(pod).throwsApiException().getObject();
            String latestPodName = latestPod.getMetadata().getName();
            LOGGER.info("latestPodName : "+latestPodName);
            return latestPodName;
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

    @Override
    public String getContainerStatus(String podName) {
        CoreV1Api api = new CoreV1Api(client);
        try {
            V1PodList list = api.listNamespacedPod(nameSpace, "false", null, null, null, null, null, null, null, null,
                    null);
            for (V1Pod pod : list.getItems()) {
                if (pod.getMetadata().getName().equals(podName)) {
                    return pod.getStatus().getPhase();
                }
            }
        } catch (ApiException e) {
            LOGGER.error("Got an exception while trying to get the status of \"" + podName + "\". Returning null.", e);
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
