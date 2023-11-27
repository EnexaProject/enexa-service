package eu.enexa.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import eu.enexa.service.BootApplication;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = BootApplication.class)
public class ContainerManagerImplTest {
    @Test
    public void testStartContainerWithVariables() throws IOException {
        ContainerManagerImpl cm = new ContainerManagerImpl();
        String imageName = "hello-world";
        String name = "test"+ UUID.randomUUID().toString();
        List<AbstractMap.SimpleEntry<String,String>> variables = new ArrayList<>();
        variables.add(new AbstractMap.SimpleEntry<>("FIRST_VARIABLE","FIRST_VARIABLE_VALUE"));
        variables.add(new AbstractMap.SimpleEntry<>("SECOND_VARIABLE","SECOND_VARIABLE_VALUE"));
        String containerId = cm.startContainer(imageName, name, variables,null,null);
        Assert.assertTrue(containerId.length() > 5);
        //TODO : need removed after test
    }

    @Test
    public void containerStatusShouldReturnStatusForExistContainers() throws InterruptedException {
        // create a client and get a list of all containers
        DockerClientConfig standard = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(standard.getDockerHost())
            .sslConfig(standard.getSSLConfig())
            .build();

        DockerClient dockerClient =  DockerClientImpl.getInstance(standard, httpClient);
        List<Container> containers = dockerClient.listContainersCmd()
            .withShowAll(true)
            .exec();

        ContainerManagerImpl cm = new ContainerManagerImpl();
        // if there is a container to do the test over it
        if(containers.size()>0){
            // select first container
            String containerId = containers.get(0).getId();
            String status = cm.getContainerStatus(containerId);
            Assert.assertEquals(containers.get(0).getState(), status);
        }else{
            //if there is no containers then we create one first then wait it to exit and do assertion
            String imageName = "hello-world";
            String name = "test"+ UUID.randomUUID().toString();
            String containerId = cm.startContainer(imageName, name, null,null,null);
            // wait to container exit
            TimeUnit.SECONDS.sleep(1);
            String status = cm.getContainerStatus(containerId);
            Assert.assertTrue( status.equals("exited"));
        }
    }

    @Test
    public void containerStatusShouldReturnNull() {
        ContainerManagerImpl cm = new ContainerManagerImpl();
        String status = cm.getContainerStatus("SomeUnknownContainerId");
        Assert.assertEquals(null, status);
    }

    @BeforeClass
    public static void setUpEnvironment() {
        // Set up your environment variables for testing here
        System.setProperty("ENEXA_META_DATA_ENDPOINT", "http://test.com");
        System.setProperty("ENEXA_MODULE_DIRECTORY", "/home/farshad/test/enexa/shared");
        System.setProperty("ENEXA_META_DATA_GRAPH", "defaultGraph");
        System.setProperty("ENEXA_RESOURCE_NAMESPACE", "defaultNameSpace");
    }

    @AfterClass
    public static void tearDownEnvironment() {
        // Clean up or reset your environment variables here
        System.clearProperty("ENEXA_META_DATA_ENDPOINT");
        System.clearProperty("ENEXA_MODULE_DIRECTORY");
        System.clearProperty("ENEXA_META_DATA_GRAPH");
        System.clearProperty("ENEXA_RESOURCE_NAMESPACE");
    }
}
