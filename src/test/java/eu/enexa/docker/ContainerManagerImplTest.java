package eu.enexa.docker;

import eu.enexa.service.BootApplication;
import org.junit.Assert;
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
        String imageName = "busybox";
        String name = "test"+ UUID.randomUUID().toString();
        List<AbstractMap.SimpleEntry<String,String>> variables = new ArrayList<>();
        variables.add(new AbstractMap.SimpleEntry<>("FIRST_VARIABLE","FIRST_VARIABLE_VALUE"));
        variables.add(new AbstractMap.SimpleEntry<>("SECOND_VARIABLE","SECOND_VARIABLE_VALUE"));
        String posName = cm.startContainer(imageName, name, variables);
        Assert.assertEquals(posName, name);
        //TODO : need this pod removed after test
    }
}
