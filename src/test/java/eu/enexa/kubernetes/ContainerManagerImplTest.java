package eu.enexa.kubernetes;
import eu.enexa.service.BootApplication;
import eu.enexa.service.web.EnexaController;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = BootApplication.class)
public class ContainerManagerImplTest {
    @Test
    public void testStartContainer() throws IOException {
        String imageName = "busybox";
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        ContainerManagerImpl cm = new ContainerManagerImpl(client);
        String name = "test"+ UUID.randomUUID().toString();
        String posName = cm.startContainer(imageName,name);
        Assert.assertEquals(posName, name);
        //TODO : need this pod removed after test
    }
}
