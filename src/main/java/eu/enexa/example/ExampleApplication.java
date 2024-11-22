package eu.enexa.example;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jenax.arq.connection.core.QueryExecutionFactory;
import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.enexa.vocab.Algorithm;
import org.dice_research.enexa.vocab.ENEXA;
import org.dice_research.rdf.RdfHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleApplication implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleApplication.class);

    private static final String SHARED_DIR_PREFIX = "enexa-dir:/";

    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_RUNNING = "Running";

    private final CloseableHttpClient client;
    private final String enexaURL;
    private final String appPath;
    private String experimentIRI;
    private String instanceIRI;
    private String metaDataEndpoint;
    private String metaDataGraph;
    private String jsonFileLocation;

    private String urlsIri;
    private String jsonIri;

    private final String appName = "app1";

    public ExampleApplication(String enexaURL, String sharedDirPath, String appPath) {
        super();
        this.enexaURL = enexaURL;
        if (sharedDirPath.endsWith(File.separator)) {
        } else {
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
        Resource expResource = RdfHelper.getSubjectResource(model, RDF.type, ENEXA.Experiment);
        if (expResource == null) {
            throw new IllegalStateException("Couldn't find experiment resource.");
        }
        experimentIRI = expResource.getURI();
        LOGGER.info("Started an experiment: {}", experimentIRI);
        // Get meta data endpoint and graph
        Resource resource = RdfHelper.getObjectResource(model, expResource, ENEXA.metaDataEndpoint);
        if (resource == null) {
            throw new IllegalStateException("Couldn't find the experiment's meta data endpoint.");
        }
        metaDataEndpoint = resource.getURI();
        resource = RdfHelper.getObjectResource(model, expResource, ENEXA.metaDataGraph);
        if (resource == null) {
            throw new IllegalStateException("Couldn't find the experiment's meta data graph.");
        }
        metaDataGraph = resource.getURI();
        LOGGER.info("Meta data can be found at {} in graph {}", metaDataEndpoint, metaDataGraph);
    }

    //todo why no usage ?
    public void addKGFile(String kgFile) throws Exception {
        String metaFilePath = kgFile;
        // Move file if it is not located in the shared directory
        if (!metaFilePath.startsWith(appPath)) {
            File kgf = new File(kgFile);
            File dest = new File(appPath + File.separator+ experimentIRI.replace("http://","") +File.separator + kgf.getName());
            try {
                //Files.copy(kgf.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                FileUtils.copyDirectory(kgf, dest);
            } catch (IOException e) {
                throw new IOException("Couldn't copy the kg file into the shared directory.", e);
            }
            metaFilePath = dest.getAbsolutePath();
        }
        // Get relative path in the shared directory
        jsonFileLocation = SHARED_DIR_PREFIX + metaFilePath.substring(appPath.length());

        // Create a model with the meta data of our file
        Model fileDescription = ModelFactory.createDefaultModel();
        // The file itself will be a blank node
        Resource file = fileDescription.createResource();
        fileDescription.add(file, RDF.type, fileDescription.createResource("http://www.w3.org/ns/prov#Entity"));
        fileDescription.add(file, ENEXA.experiment, fileDescription.createResource(experimentIRI));
        fileDescription.add(file, ENEXA.location, fileDescription.createLiteral(jsonFileLocation));
        fileDescription.add(file, fileDescription.createProperty("http://www.w3.org/ns/dcat#mediaType"),
                fileDescription.createResource("https://www.iana.org/assignments/media-types/text/turtle"));

        // Send the model
        Model response = requestRDF(enexaURL + "/add-resource", fileDescription);

        if (response == null) {
            throw new IllegalStateException("Couldn't add a resource to the meta data.");
        }

        // Get the new IRI of the resource
        Resource fileResource = RdfHelper.getSubjectResource(response, RDF.type,
                response.createResource("http://www.w3.org/ns/prov#Entity"));
        if (fileResource == null) {
            throw new IllegalStateException("Couldn't find the file resource.");
        }
        LOGGER.info("File resource {} has been created.", fileResource.getURI());
        //String kgFileIri = fileResource.getURI();
        //
    }

    public void addUrls(String urlsFile, String moduleName) throws Exception {
        // Move file if it is not located in the shared directory
        File urls = new File(urlsFile);
        File dest = new File(appPath + File.separator+ appName+File.separator+ experimentIRI.split("/")[experimentIRI.split("/").length -1] +File.separator +moduleName +File.separator+urls.getName());
        try {
            FileUtils.copyFile(urls, dest);
        } catch (IOException e) {
            throw new IOException("Couldn't copy the kg file into the shared directory.", e);
        }

        String destinationUrlsFile = dest.getAbsolutePath();

        // Get relative path in the shared directory
        jsonFileLocation = SHARED_DIR_PREFIX + destinationUrlsFile.substring(appPath.length());

        // Create a model with the meta data of our file
        Model fileDescription = ModelFactory.createDefaultModel();
        // The file itself will be a blank node
        String addFile = "@prefix enexa:  <http://w3id.org/dice-research/enexa/ontology#> .\n" +
            "  @prefix prov:   <http://www.w3.org/ns/prov#> .\n" +
            "\n" +
            "  [] a prov:Entity ; \n" +
            "      enexa:experiment <"+experimentIRI+"> ; \n" +
            "      enexa:location \""+ jsonFileLocation +"\" .";
        fileDescription.read(new java.io.StringReader(addFile),null,"TURTLE");

        // Send the model
        Model response = requestRDF(enexaURL + "/add-resource", fileDescription);

        if (response == null) {
            throw new IllegalStateException("Couldn't add a resource to the meta data.");
        }

        // Get the new IRI of the resource
        Resource fileResource = RdfHelper.getSubjectResource(response, RDF.type,
            response.createResource("http://www.w3.org/ns/prov#Entity"));
        if (fileResource == null) {
            throw new IllegalStateException("Couldn't find the file resource.");
        }
        LOGGER.info("File resource {} has been created.", fileResource.getURI());
        urlsIri = fileResource.getURI();
    }

    public void addJson(String jsonFile, String moduleName) throws Exception {
        // Move file if it is not located in the shared directory
        File json = new File(jsonFile);
        File dest = new File(appPath + File.separator+ appName+File.separator+experimentIRI.split("/")[experimentIRI.split("/").length -1] +File.separator +moduleName +File.separator+json.getName());
        try {
            FileUtils.copyFile(json, dest);
        } catch (IOException e) {
            throw new IOException("Couldn't copy the kg file into the shared directory.", e);
        }

        String jsonFileDestination = dest.getAbsolutePath();

        // Get relative path in the shared directory
        jsonFileLocation = SHARED_DIR_PREFIX + jsonFileDestination.substring(appPath.length());

        // Create a model with the meta data of our file
        Model fileDescription = ModelFactory.createDefaultModel();
        // The file itself will be a blank node
        String addFile = "@prefix enexa:  <http://w3id.org/dice-research/enexa/ontology#> .\n" +
            "  @prefix prov:   <http://www.w3.org/ns/prov#> .\n" +
            "\n" +
            "  [] a prov:Entity ; \n" +
            "      enexa:experiment <"+experimentIRI+"> ; \n" +
            "      enexa:location \""+ jsonFileLocation +"\" .";
        fileDescription.read(new java.io.StringReader(addFile),null,"TURTLE");

        // Send the model
        Model response = requestRDF(enexaURL + "/add-resource", fileDescription);

        if (response == null) {
            throw new IllegalStateException("Couldn't add a resource to the meta data.");
        }

        // Get the new IRI of the resource
        Resource fileResource = RdfHelper.getSubjectResource(response, RDF.type,
            response.createResource("http://www.w3.org/ns/prov#Entity"));
        if (fileResource == null) {
            throw new IllegalStateException("Couldn't find the file resource.");
        }
        LOGGER.info("File resource {} has been created.", fileResource.getURI());
        jsonIri = fileResource.getURI();
    }

    private void startExtraction() throws Exception {
        Model instanceModel = ModelFactory.createDefaultModel();

        String start_module_message = "@prefix alg: <http://www.w3id.org/dice-research/ontologies/algorithm/2023/06/> .\n" +
            "@prefix enexa:  <http://w3id.org/dice-research/enexa/ontology#> .\n" +
            "@prefix prov:   <http://www.w3.org/ns/prov#> .\n" +
            "@prefix hobbit: <http://w3id.org/hobbit/vocab#> . \n" +
            "@prefix rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
            "[] rdf:type enexa:ModuleInstance ; " +
            "enexa:experiment <"+experimentIRI+"> ; " +
            "alg:instanceOf <http://w3id.org/dice-research/enexa/module/extraction/1.0.0> ; " +
            "<http://w3id.org/dice-research/enexa/module/extraction/parameter/urls_to_process> <"+urlsIri+">;" +
            "<http://w3id.org/dice-research/enexa/module/extraction/parameter/path_generation_parameters> <"+jsonIri+">.";

        instanceModel.read(new java.io.StringReader(start_module_message), null, "TURTLE");

        Model response = requestRDF(enexaURL + "/start-container", instanceModel);

        if (response == null) {
            throw new IllegalStateException("Couldn't start a container.");
        }
        // Get the new IRI of the newly created module instance
        Resource instanceResource = RdfHelper.getSubjectResource(response, RDF.type, ENEXA.ModuleInstance);
        if (instanceResource == null) {
            throw new IllegalStateException("Couldn't find module instance resource.");
        }
        instanceIRI = instanceResource.getURI();
        LOGGER.info("module instance {} has been created.", instanceIRI);
    }
    private void startEmbeddingGeneration() throws Exception {
        // Create a model with the meta data of the module that we want to run
        Model instanceModel = ModelFactory.createDefaultModel();
        // The module instance itself will be a blank node
        Resource instance = instanceModel.createResource();
        instanceModel.add(instance, RDF.type, ENEXA.ModuleInstance);
        //TODO is it correct ?
        String preFix = "http://w3id.org/dice-research/enexa/module/dice-embeddings/";
        instanceModel.add(instance, Algorithm.instanceOf, instanceModel.createResource(preFix +"v1.0"));
        instanceModel.add(instance, ENEXA.experiment, instanceModel.createResource(experimentIRI));
        // Add parameters
        /*instanceModel.add(instance, instanceModel.createProperty(preFix+"parameters/model"),
                instanceModel.createResource(kgFileIri));*/
        instanceModel.add(instance, instanceModel.createProperty(preFix +"parameter/model"),
            instanceModel.createTypedLiteral("ConEx"));
        //http://module-instance-1> <parameters/path_dataset_folder> <http://aresource> .
        instanceModel.add(instance,instanceModel.createProperty(preFix +"parameter/path_dataset_folder"),instanceModel.createResource(jsonFileLocation.replace("enexa-dir:","http:")));
//<http://aresource> enexa:location "enexa-dir://something" .
        instanceModel.add(instanceModel.createResource(jsonFileLocation.replace("enexa-dir:","http:")),ENEXA.location,instanceModel.createLiteral(jsonFileLocation));

        instanceModel.add(instance, instanceModel.createProperty(preFix +"parameter/num_epochs"),
                instanceModel.createTypedLiteral(5));
        instanceModel.add(instance, instanceModel.createProperty(preFix +"parameter/embedding_dim"),
                instanceModel.createTypedLiteral(2));

        // Send the model
        Model response = requestRDF(enexaURL + "/start-container", instanceModel);

        if (response == null) {
            throw new IllegalStateException("Couldn't start a container.");
        }
        // Get the new IRI of the newly created module instance
        Resource instanceResource = RdfHelper.getSubjectResource(response, RDF.type, ENEXA.ModuleInstance);
        if (instanceResource == null) {
            throw new IllegalStateException("Couldn't find module instance resource.");
        }
        instanceIRI = instanceResource.getURI();
        LOGGER.info("module instance {} has been created.", instanceIRI);
    }

    protected Model requestRDF(String url, Model data) {
        HttpPost request = new HttpPost(url);
        request.setHeader("Accept", "application/ld+json");
        request.setHeader("Content-type", "application/ld+json");
        if (data != null) {
            try (StringWriter writer = new StringWriter()) {
                data.write(writer, "JSON-LD");
                request.setEntity(new StringEntity(writer.toString()));
            } catch (IOException e) {
                LOGGER.error("Catched unexpected exception while adding data to the request. Returning null.", e);
                return null;
            }
        }
        Model model = null;
        try (CloseableHttpResponse httpResponse = client.execute(request)) {
            if (httpResponse.getCode() >= 300) {
                throw new IllegalStateException("Received HTTP response with code " + httpResponse.getCode());
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
                throw new IllegalStateException("Couldn't get the status of a container.");
            }
            // Get the new IRI of the newly created module instance
            status = RdfHelper.getStringValue(requestModel, instance, ENEXA.containerStatus);
            if (status == null) {
                throw new IllegalStateException("Couldn't find the status of the module instance resource.");
            }
            if (status.equals(STATUS_PENDING) || status.equals(STATUS_RUNNING)) {
                Thread.sleep(1000);
            } else {
                return;
            }
        }
    }

    private void queryFilePath() throws Exception {
        try (QueryExecutionFactory queryExecFactory = new QueryExecutionFactoryHttp(metaDataEndpoint, metaDataGraph)) {
            QueryExecution qe = queryExecFactory.createQueryExecution("SELECT ?fileIri ?fileLocation WHERE {" + "<"
                    + instanceIRI + "> <http://example.org/dicee/parameter/model.pt> ?fileIri . "
                    + "?fileIri <http://w3id.org/dice-research/enexa/ontology#location> ?fileLocation . " + "}");
            ResultSet rs = qe.execSelect();
            if (rs.hasNext()) {
                QuerySolution qs = rs.next();
                String resultFileIri = qs.getResource("fileIri").getURI();
                String resultFileLocation = qs.getLiteral("fileLocation").getString();
                LOGGER.info("Result file {} located at {}.", resultFileIri, resultFileLocation);
            } else {
                LOGGER.error("Couldn't get the expected result file from the meta data endpoint.");
            }
        }
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
            String moduleName = "extraction";
            app.addJson("/home/farshad/test/enexa/shared/generation_parameters.json", moduleName);
            app.addUrls("/home/farshad/test/enexa/shared/wikipedia_company_urls_short.json", moduleName);
            app.startExtraction();
            // 4. Start the embedding generation
            //app.startEmbeddingGeneration();
            // 5. Wait for the embedding module to be finish
            //app.waitForEmbeddings();
            // 6. Get the output of the embedding algorithm
            // app.queryFilePath();
            // 7. Cleanup
            //app.finishExperiment();
        } catch (Exception e) {
            LOGGER.error("Something went wrong :-( ", e);
        }
    }
}
