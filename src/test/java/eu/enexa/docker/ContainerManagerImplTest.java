package eu.enexa.docker;

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
        String posName = cm.startContainer(imageName, name, variables);
        Assert.assertTrue(posName.length() > 5);
        //TODO : need removed after test
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
