package eu.enexa.example;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.rdf.RdfHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.enexa.vocab.ENEXA;

public class ExampleApplication implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleApplication.class);

    private static final String SHARED_DIR_PREFIX = "enexa-dir:";

    private static final String STATUS_RUNNING = "running";

    private CloseableHttpClient client;
    private String enexaURL;
    private String sharedDirPath;
    private String appPath;
    private String experimentIRI;
    private String instanceIRI;
    private String metaDataEndpoint;
    private String metaDataGraph;
    private String kgFileIri;
    private String kgFileLocation;

    public ExampleApplication(String enexaURL, String sharedDirPath, String appPath) {
        super();
        this.enexaURL = enexaURL;
        if (sharedDirPath.endsWith(File.separator)) {
            this.sharedDirPath = sharedDirPath.substring(0, sharedDirPath.length() - 1);
        } else {
            this.sharedDirPath = sharedDirPath;
        }
        if (appPath.endsWith(File.separator)) {
            this.appPath = appPath.substring(0, appPath.length() - 1);
        } else {
            this.appPath = appPath;
        }

        client = HttpClients.createDefault();
    }

    public void startExperiment() throws Exception {
        Model model = requestRDF(enexaURL + "/start-experiment", null);
        if (model == null) {
            throw new IOException("Couldn't create experiment.");
        }
        Resource expResource = RdfHelper.getSubjectResource(model, RDF.type, ENEXA.experiment);
        if (expResource == null) {
            throw new Exception("Couldn't find experiment resource.");
        }
        experimentIRI = expResource.getURI();
        LOGGER.info("Started an experiment: {}", experimentIRI);
        // Get meta data endpoint and graph
        Resource resource = RdfHelper.getObjectResource(model, expResource, ENEXA.metaDataEndpoint);
        if (resource == null) {
            throw new Exception("Couldn't find the experiment's meta data endpoint.");
        }
        metaDataEndpoint = resource.getURI();
        resource = RdfHelper.getObjectResource(model, expResource, ENEXA.metaDataGraph);
        if (resource == null) {
            throw new Exception("Couldn't find the experiment's meta data graph.");
        }
        metaDataGraph = resource.getURI();
        LOGGER.info("Meta data can be found at {} in graph {}", metaDataEndpoint, metaDataGraph);
    }

    public void addKGFile(String kgFile) throws Exception {
        String metaFilePath = kgFile;
        // Move file if it is not located in the shared directory
        if (!metaFilePath.startsWith(metaFilePath)) {
            File kgf = new File(kgFile);
            File dest = new File(appPath + File.separator + kgf.getName());
            try {
                Files.copy(kgf.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new IOException("Couldn't copy the kg file into the shared directory.", e);
            }
            metaFilePath = dest.getAbsolutePath();
        }
        // Get relative path in the shared directory
        kgFileLocation = SHARED_DIR_PREFIX + metaFilePath.substring(sharedDirPath.length());

        // Create a model with the meta data of our file
        Model fileDescription = ModelFactory.createDefaultModel();
        // The file itself will be a blank node
        Resource file = fileDescription.createResource();
        fileDescription.add(file, RDF.type, fileDescription.createResource("http://www.w3.org/ns/prov#Entity"));
        fileDescription.add(file, ENEXA.experiment, fileDescription.createResource(experimentIRI));
        fileDescription.add(file, ENEXA.location, fileDescription.createLiteral(kgFileLocation));
        fileDescription.add(file, fileDescription.createProperty("http://www.w3.org/ns/dcat#mediaType"),
                fileDescription.createResource("https://www.iana.org/assignments/media-types/text/turtle"));

        // Send the model
        Model response = requestRDF(enexaURL + "/add-resource", fileDescription);

        if (response == null) {
            throw new Exception("Couldn't add a resource to the meta data.");
        }
        // Get the new IRI of the resource
        Resource fileResource = RdfHelper.getSubjectResource(response, RDF.type,
                response.createResource("http://www.w3.org/ns/prov#Entity"));
        if (fileResource == null) {
            throw new Exception("Couldn't find the file resource.");
        }
        LOGGER.info("File resource {} has been created.", fileResource.getURI());
    }

    private void startEmbeddingGeneration() throws Exception {
        // Create a model with the meta data of the module that we want to run
        Model instanceModel = ModelFactory.createDefaultModel();
        // The module instance itself will be a blank node
        Resource instance = instanceModel.createResource();
        instanceModel.add(instance, RDF.type, ENEXA.ModuleInstance);
        instanceModel.add(instance, ENEXA.experiment, instanceModel.createResource(experimentIRI));
        // TODO Add parameters
        // instanceModel.add(instance, ENEXA.location,
        // instanceModel.createLiteral(kgFileLocation));
        // instanceModel.add(instance,
        // instanceModel.createProperty("http://www.w3.org/ns/dcat#mediaType"),
        // instanceModel.createResource("https://www.iana.org/assignments/media-types/text/turtle"));

        // Send the model
        Model response = requestRDF(enexaURL + "/start-container", instanceModel);

        if (response == null) {
            throw new Exception("Couldn't start a container.");
        }
        // Get the new IRI of the newly created module instance
        Resource instanceResource = RdfHelper.getSubjectResource(response, RDF.type, ENEXA.ModuleInstance);
        if (instanceResource == null) {
            throw new Exception("Couldn't find module instance resource.");
        }
        instanceIRI = instanceResource.getURI();
        LOGGER.info("module instance {} has been created.", instanceIRI);
    }

    public void finishExperiment() {
        // TODO Auto-generated method stub

    }

    protected Model requestRDF(String url, Model data) {
        HttpPost request = new HttpPost(url);
        request.addHeader("Accept", "application/ld+json");
        if (data != null) {
            try (StringWriter writer = new StringWriter()) {
                request.addHeader("Content-type", "application/ld+json");
                data.write(writer, "JSON-LD");
                request.setEntity(new StringEntity(writer.toString()));
            } catch (IOException e) {
                LOGGER.error("Catched unexpected exception while adding data to the request. Returning null.", e);
                return null;
            }
        }
        Model model = null;
        try (CloseableHttpResponse httpResponse = client.execute(request)) {
            if (httpResponse.getStatusLine().getStatusCode() >= 300) {
                throw new IllegalStateException(
                        "Received HTTP response with code " + httpResponse.getStatusLine().getStatusCode());
            }

            try (InputStream is = httpResponse.getEntity().getContent()) {
                model = ModelFactory.createDefaultModel();
                model.read(is, "", "JSON-LD");
            }
        } catch (Exception e) {
            LOGGER.error("Caught an exception while running request. Returning null.");
            return null;
        }
        return model;
    }

    private void waitForEmbeddings() throws Exception {
        Model requestModel = ModelFactory.createDefaultModel();
        Resource instance = requestModel.createResource(instanceIRI);
        requestModel.add(instance, RDF.type, ENEXA.ModuleInstance);
        requestModel.add(instance, ENEXA.experiment, requestModel.createResource(experimentIRI));

        String status = null;
        while (true) {
            Model response = requestRDF(enexaURL + "/container-status", requestModel);

            if (response == null) {
                throw new Exception("Couldn't get the status of a container.");
            }
            // Get the new IRI of the newly created module instance
            status = RdfHelper.getStringValue(requestModel, instance, ENEXA.containerStatus);
            if (status == null) {
                throw new Exception("Couldn't find the status of the module instance resource.");
            }
            if (STATUS_RUNNING.equals(status)) {
                Thread.sleep(1000);
            } else {
                return;
            }
        }
    }

    private void queryFilePath() {
        Model requestModel = ModelFactory.createDefaultModel();
        Resource instance = requestModel.createResource(instanceIRI);
        requestModel.add(instance, RDF.type, ENEXA.ModuleInstance);
        requestModel.add(instance, ENEXA.experiment, requestModel.createResource(experimentIRI));
        // TODO Add resultIRI for the file

        // Send the model
        Model response = requestRDF(enexaURL + "/start-container", requestModel);

        if (response == null) {
            throw new Exception("Couldn't start a container.");
        }
        // Get the new IRI of the newly created module instance
        Resource instanceResource = RdfHelper.getSubjectResource(response, RDF.type, ENEXA.ModuleInstance);
        if (instanceResource == null) {
            throw new Exception("Couldn't find module instance resource.");
        }
        instanceIRI = instanceResource.getURI();
        LOGGER.info("module instance {} has been created.", instanceIRI);
    }

    @Override
    public void close() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println(
                    "Error: wrong usage. ClassHierarchyCollector <enexa-service-URL> <shared-directory-path> <app-directory-path> <knowledge-graph-file>");
            return;
        }
        String enexaServiceUrl = args[0];
        String sharedDirPath = args[1];
        String appPath = args[2];
        String kgFile = args[3];

        try (ExampleApplication app = new ExampleApplication(enexaServiceUrl, sharedDirPath, appPath)) {
            // 1. Pause for some seconds
            // 2. Start an experiment
            app.startExperiment();
            // 3. Add the knowledge graph file
            app.addKGFile(kgFile);
            // 4. Start the embedding generation
            app.startEmbeddingGeneration();
            // 5. Wait for the embedding module to be finish
            app.waitForEmbeddings();
            // 6. Get the output of the embedding algorithm
            app.queryFilePath();
            // 7. Cleanup
            app.finishExperiment();
        } catch (Exception e) {
            LOGGER.error("Something went wrong :-( ", e);
        }
    }
}
