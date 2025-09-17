package eu.enexa.module;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import eu.enexa.service.BootApplication;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import eu.enexa.model.ModuleModel;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

public class FileBasedModuleManagerTest {

    @Test
    public void test() throws URISyntaxException, IOException {
        URL fileUrl = this.getClass().getClassLoader().getResource("eu/enexa/module/exampleModule1.ttl");
        File file = new File(fileUrl.toURI());

        File temp = File.createTempFile("module", ".ttl");
        FileUtils.copyFile(file, temp);

        FileBasedModuleManager manager = new FileBasedModuleManager();
        manager.addFileOrDirectory(temp);

        ModuleModel module = manager.deriveModule("http://dice-research.org/DICE-framework/v1.0", null);
        Assert.assertNotNull(module);
        Assert.assertEquals("http://dice-research.org/DICE-framework/v1.0", module.getModuleIri());
        Assert.assertNull(module.getModuleUrl());
        Assert.assertEquals("urn:container:docker:image:docker.io/dicegroup/dice-embeddings:0.1.3",
                module.getImage());
    }

    // no mor exceptions , now we just log if the file is not supported
    /*@Test(expected = IOException.class)
    public void unknownFileShouldThrowException() throws URISyntaxException, IOException {
        File temp = File.createTempFile("module", ".frs");
        FileBasedModuleManager manager = new FileBasedModuleManager();
        manager.addFileOrDirectory(temp);
        Assert.fail();
    }*/

    @Test(expected = IllegalArgumentException.class)
    public void notCompletedFileShouldThrowException() throws URISyntaxException, IOException {
        URL fileUrl = this.getClass().getClassLoader().getResource("eu/enexa/module/exampleModule1_notComplete.ttl");
        File file = new File(fileUrl.toURI());

        File temp = File.createTempFile("module", ".ttl");
        FileUtils.copyFile(file, temp);

        FileBasedModuleManager manager = new FileBasedModuleManager();
        manager.addFileOrDirectory(temp);
        Assert.fail();
    }
}
