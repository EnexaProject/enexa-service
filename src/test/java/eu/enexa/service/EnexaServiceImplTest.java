package eu.enexa.service;

import eu.enexa.model.AddedResource;
import eu.enexa.model.ModuleModel;
import eu.enexa.model.ModuleNotFoundException;
import eu.enexa.model.StartContainerModel;
import org.apache.jena.rdf.model.*;
import org.dice_research.enexa.vocab.ENEXA;
import org.dice_research.enexa.vocab.HOBBIT;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EnexaServiceImpl.class})
public class EnexaServiceImplTest {

    @InjectMocks
    @Autowired
    private EnexaServiceImpl enexaServiceImpl;

    @Mock
    private ContainerManager containerManager;

    @Mock
    private MetadataManager metadataManager;

    @Mock
    private ModuleManager moduleManager;

    private static final String EXPERIMENT_IRI = "http://example.org/exp123";
    private static final String MODULE_IRI = "http://example.org/moduleX";
    private static final String MODULE_URL = "http://module.url/meta";
    private static final String META_DATA_ENDPOINT = "http://localhost:3030/enexa";
    private static final String SHARED_DIRECTORY = "/tmp/enexa";
    private static final String APP_NAME = "myApp";
    private static final String MODULE_IMAGE = "module:latest";
    private static final String INSTANCE_IRI = "http://example.org/instance1";
    private static final String CONTAINER_ID = "containerid-xyz";
    private static final String POD_NAME = "enexa-98765";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Simulate environment variables
        setEnv("ENEXA_META_DATA_ENDPOINT", META_DATA_ENDPOINT);
        setEnv("ENEXA_SHARED_DIRECTORY", SHARED_DIRECTORY);
        setEnv("ENEXA_SERVICE_URL", "http://localhost:8080");

        // Set the @Value property via reflection
        java.lang.reflect.Field appNameField = EnexaServiceImpl.class.getDeclaredField("appName");
        appNameField.setAccessible(true);
        appNameField.set(enexaServiceImpl, APP_NAME);
    }

    @After
    public void tearDown() throws Exception {
        clearEnv("ENEXA_META_DATA_ENDPOINT");
        clearEnv("ENEXA_SHARED_DIRECTORY");
        clearEnv("ENEXA_SERVICE_URL");
    }

    /**
     * Utility: Set an environment variable for the test JVM.
     */
    private static void setEnv(String key, String value) throws Exception {
        Map<String, String> env = System.getenv();
        java.lang.reflect.Field field = env.getClass().getDeclaredField("m");
        field.setAccessible(true);
        ((Map) field.get(env)).put(key, value);
    }

    /**
     * Utility: Remove an environment variable for the test JVM.
     */
    private static void clearEnv(String key) throws Exception {
        Map<String, String> env = System.getenv();
        java.lang.reflect.Field field = env.getClass().getDeclaredField("m");
        field.setAccessible(true);
        ((Map) field.get(env)).remove(key);
    }

    @Test
    public void testStartExperiment() {
        // Prepare MetadataManager mocking
        when(metadataManager.generateResourceIRI()).thenReturn(EXPERIMENT_IRI);
        Map<String, String> metaInfo = new HashMap<>();
        metaInfo.put("sparqlEndpointUrl", "http://localhost:9999/fake");
        metaInfo.put("defaultMetaDataGraphIRI", "http://graph/expm123");
        when(metadataManager.getMetadataEndpointInfo()).thenReturn(metaInfo);

        // Call service
        Model model = enexaServiceImpl.startExperiment();

        assertNotNull(model);
        Resource expRes = model.getResource(EXPERIMENT_IRI);
        assertTrue(model.contains(expRes, org.apache.jena.vocabulary.RDF.type, ENEXA.Experiment));
        verify(metadataManager, times(1)).addMetaData(any(Model.class));
    }

    @Test
    public void testGetMetadataEndpoint() {
        Map<String, String> info = new HashMap<>();
        info.put("sparqlEndpointUrl", "http://localhost:3030/meta");
        info.put("defaultMetaDataGraphIRI", "http://graph/g1");
        when(metadataManager.getMetadataEndpointInfo()).thenReturn(info);

        Model model = enexaServiceImpl.getMetadataEndpoint(EXPERIMENT_IRI);

        assertNotNull(model);
        Resource res = model.getResource(EXPERIMENT_IRI);
        assertTrue(model.contains(res, org.apache.jena.vocabulary.RDF.type, ENEXA.Experiment));
        assertTrue(model.listStatements(res, ENEXA.metaDataEndpoint, (RDFNode)null).hasNext());
        assertTrue(model.listStatements(res, ENEXA.metaDataGraph, (RDFNode)null).hasNext());
    }

    @Test
    public void testStartContainer_HappyPath() throws Exception {
        StartContainerModel scm = mock(StartContainerModel.class);
        ModuleModel module = mock(ModuleModel.class);

        when(scm.getModuleIri()).thenReturn(MODULE_IRI);
        when(scm.getModuleUrl()).thenReturn(MODULE_URL);
        when(moduleManager.deriveModule(MODULE_IRI, MODULE_URL)).thenReturn(module);
        when(module.getModuleIri()).thenReturn(MODULE_IRI);
        when(module.getImage()).thenReturn(MODULE_IMAGE);
        when(module.getPort()).thenReturn(null);

        when(metadataManager.generateResourceIRI()).thenReturn(INSTANCE_IRI);
        Model model = ModelFactory.createDefaultModel();
        when(scm.getModel()).thenReturn(model);
        when(scm.getExperiment()).thenReturn(EXPERIMENT_IRI);

        Map<String, String> metaInfo = new HashMap<>();
        metaInfo.put("defaultMetaDataGraphIRI", "http://graph/exp123");
        when(metadataManager.getMetadataEndpointInfo()).thenReturn(metaInfo);

        when(containerManager.startContainer(anyString(), anyString(), anyList(), anyString(), anyString(), anyMap())).thenReturn(CONTAINER_ID);

        Model returnedModel = enexaServiceImpl.startContainer(scm);

        assertNotNull(returnedModel);
        Resource instRes = returnedModel.getResource(INSTANCE_IRI);
        assertTrue(returnedModel.contains(instRes, ENEXA.containerId, CONTAINER_ID));
        assertTrue(returnedModel.contains(instRes, HOBBIT.startTime, (RDFNode)null));
        verify(metadataManager, atLeastOnce()).addMetaData(any(Model.class));
    }

    @Test(expected = ModuleNotFoundException.class)
    public void testStartContainer_ModuleNotFound() throws Exception {
        StartContainerModel scm = mock(StartContainerModel.class);
        when(scm.getModuleIri()).thenReturn(MODULE_IRI);
        when(scm.getModuleUrl()).thenReturn(MODULE_URL);
        when(moduleManager.deriveModule(MODULE_IRI, MODULE_URL)).thenThrow(new RuntimeException("Not found"));

        enexaServiceImpl.startContainer(scm);
    }

    @Test
    public void testAddResource1() {
        // getSubjectResource static method is called
        // For simplicity just prepare the minimal RDF
        Model req = ModelFactory.createDefaultModel();
        Resource oldRes = req.createResource(EXPERIMENT_IRI);
        req.add(oldRes, ENEXA.experiment, "bla");

        when(metadataManager.generateResourceIRI()).thenReturn("http://example.org/newRes123");

        AddedResource ar = enexaServiceImpl.addResource(req);

        assertNotNull(ar);
        assertNotNull(ar.getModel());
    }

    @Test
    public void testContainerStatus() {
        when(metadataManager.getContainerName(EXPERIMENT_IRI, INSTANCE_IRI)).thenReturn(POD_NAME);
        when(containerManager.getContainerStatus(POD_NAME)).thenReturn("running");

        Model result = enexaServiceImpl.containerStatus(EXPERIMENT_IRI, INSTANCE_IRI);

        assertNotNull(result);
        Resource inst = result.getResource(INSTANCE_IRI);
        assertTrue(result.contains(inst, ENEXA.experiment, result.getResource(EXPERIMENT_IRI)));
        assertTrue(result.contains(inst, ENEXA.containerStatus, "running"));
    }

    @Test
    public void testStopContainer() {
        Model result = enexaServiceImpl.stopContainer(EXPERIMENT_IRI, CONTAINER_ID);
        assertNotNull(result);
        assertTrue(result.contains(result.getResource(CONTAINER_ID), ENEXA.experiment, result.getResource(EXPERIMENT_IRI)));
    }

    @Test
    public void testFinishExperiment() {
        List<String> containerNames = Arrays.asList("pod1", "pod2");
        when(metadataManager.getAllContainerNames(EXPERIMENT_IRI)).thenReturn(containerNames);
        when(containerManager.stopContainer(anyString())).thenReturn("STOPPED_OK");

        Model model = enexaServiceImpl.finishExperiment(EXPERIMENT_IRI);

        for(String containerName : containerNames) {
            assertTrue(model.contains(model.getResource(containerName), ENEXA.containerName, model.getResource(EXPERIMENT_IRI)));
        }
    }

    @Test
    public void testGeneratePodName() {
        String name = enexaServiceImpl.generatePodName(MODULE_IRI);
        assertTrue(name.startsWith("enexa-"));
    }
}
