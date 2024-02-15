package eu.enexa.example;

import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.pagination.core.QueryExecutionFactoryPaginated;
import org.aksw.jenax.arq.connection.core.QueryExecutionFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.jena.base.Sys;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.DatasetDescription;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.enexa.vocab.ENEXA;
import org.dice_research.rdf.RdfHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.http.HttpClient;
public class SimpleClient implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleClient.class);
    private CloseableHttpClient client;
    private static final String SHARED_DIR_PREFIX = "enexa-dir:/";
    private String experimentIRI;
    //private String instanceIRI;
    private String metaDataEndpoint;
    private String metaDataGraph;
    private QueryExecutionFactory queryExecFactory;
    private String enexaURL = "http://localhost:8081";
    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_RUNNING = "Running";
    private final String appName = "app3";

    private final String appPath = "/home/farshad/test/enexa/shared";

    /**
     * A simple application demonstrating the development of a client that utilizes the ENEXA service.
     */
    public SimpleClient() {
        client = HttpClients.createDefault();

    }

    /**
     *  Send a start experiment request to the service and set the experimentIRI, metaDataEndpoint, and metaDataGraph parameters.
     */

    public void startExperiment() throws Exception {
        Model model = requestRDF(enexaURL + "/start-experiment", null);
        if (model == null) {
            throw new IOException("Couldn't create experiment.");
        }
        Resource expResource = RdfHelper.getSubjectResource(model, RDF.type, ENEXA.Experiment);
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


    /*protected String requestGet(String url) {
        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse httpResponse = client.execute(request)) {
            if (httpResponse.getCode() >= 300) {
                throw new IllegalStateException("Received HTTP response with code " + httpResponse.getCode());
            }

            try (InputStream is = httpResponse.getEntity().getContent()) {
                return IOUtils.toString(is, "UTF-8");
            }
        } catch (Exception e) {
            LOGGER.error("Caught an exception while running request. Returning null.");
            return null;
        }
    }*/


    /**
     *  Send an HTTP GET request and return the result as a Model.
     *  @param url  the URL to which the request is sent
     */
    protected Model requestGetRecieveModel(String url) {
        HttpGet request = new HttpGet(url);
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

    /**
     * Sends an HTTP POST request to the specified URL with the given request body.
     *
     * @param url  the URL to which the request is sent
     * @param body the body of the HTTP POST request
     * @return the response body of the HTTP POST request
     * @throws IllegalStateException if the HTTP response code is greater than or equal to 300
     */
    protected String requestPost(String url, String body) {
        HttpPost request = new HttpPost(url);
        request.setHeader("Accept", "text/turtle");
        request.setHeader("Content-type", "application/json");
        // Set the request body
        StringEntity stringEntity = new StringEntity(body);
        request.setEntity(stringEntity);

        try (CloseableHttpResponse httpResponse = client.execute(request)) {
            if (httpResponse.getCode() >= 300) {
                throw new IllegalStateException("Received HTTP response with code " + httpResponse.getCode());
            }

            try (InputStream is = httpResponse.getEntity().getContent()) {
                return IOUtils.toString(is, "UTF-8");
            }
        } catch (Exception e) {
            LOGGER.error("Caught an exception while running request. Returning null.");
            return null;
        }
    }

    /**
     * Sends an HTTP POST request to the specified URL with optional RDF data, expecting RDF data in response.
     *
     * @param url  the URL to which the request is sent
     * @param data the RDF data to include in the request (optional)
     * @return a Model object containing the RDF data received in response, or null if an error occurs
     * @throws IllegalStateException if the HTTP response code is greater than or equal to 300
     */
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

    /**
     * Adds a file to the Enexa and returns its IRI.
     *
     * @param fileToAdd   the path of the file to be added
     * @param moduleName  the name of the module to which the file belongs
     * @return the IRI of the added file
     * @throws Exception if an error occurs during the file addition process
     */
    public String addFile(String fileToAdd, String moduleName) throws Exception {
        // Move file if it is not located in the shared directory
        File json = new File(fileToAdd);
        File dest = new File(appPath + File.separator+ appName+File.separator+experimentIRI.split("/")[experimentIRI.split("/").length -1] +File.separator +moduleName +File.separator+json.getName());
        try {
            FileUtils.copyFile(json, dest);
        } catch (IOException e) {
            throw new IOException("Couldn't copy the kg file into the shared directory.", e);
        }

        String jsonFileDestination = dest.getAbsolutePath();

        // Get relative path in the shared directory
        String addedFileLocation = SHARED_DIR_PREFIX + jsonFileDestination.substring(appPath.length());

        // Create a model with the meta data of our file
        Model fileDescription = ModelFactory.createDefaultModel();
        // The file itself will be a blank node
        String addFile = "@prefix enexa:  <http://w3id.org/dice-research/enexa/ontology#> .\n" +
            "  @prefix prov:   <http://www.w3.org/ns/prov#> .\n" +
            "\n" +
            "  [] a prov:Entity ; \n" +
            "      enexa:experiment <"+experimentIRI+"> ; \n" +
            "      enexa:location \""+ addedFileLocation +"\" .";
        fileDescription.read(new java.io.StringReader(addFile),null,"TURTLE");

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
        return fileResource.getURI();
    }

    /**
     * Starts the extraction process in the Enexa  with the specified parameters and returns the IRI of the instance.
     *
     * @param iriAddedFile          the IRI of the file added for extraction
     * @param iriGenerationParameter the IRI of the generation parameter
     * @return the IRI of the newly created module instance
     * @throws Exception if an error occurs during the extraction process
     */
    private String startExtraction(String iriAddedFile , String iriGenerationParameter) throws Exception {
        Model instanceModel = ModelFactory.createDefaultModel();

        String start_module_message = "@prefix alg: <http://www.w3id.org/dice-research/ontologies/algorithm/2023/06/> .\n" +
            "@prefix enexa:  <http://w3id.org/dice-research/enexa/ontology#> .\n" +
            "@prefix prov:   <http://www.w3.org/ns/prov#> .\n" +
            "@prefix hobbit: <http://w3id.org/hobbit/vocab#> . \n" +
            "@prefix rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
            "[] rdf:type enexa:ModuleInstance ; " +
            "enexa:experiment <"+experimentIRI+"> ; " +
            "alg:instanceOf <http://w3id.org/dice-research/enexa/module/extraction/1.0.0> ; " +
            "<http://w3id.org/dice-research/enexa/module/extraction/parameter/urls_to_process> <"+iriAddedFile+">;" +
            "<http://w3id.org/dice-research/enexa/module/extraction/parameter/path_generation_parameters> <"+iriGenerationParameter+">.";

        instanceModel.read(new java.io.StringReader(start_module_message), null, "TURTLE");

        Model response = requestRDF(enexaURL + "/start-container", instanceModel);

        if (response == null) {
            throw new Exception("Couldn't start a container.");
        }
        // Get the new IRI of the newly created module instance
        Resource instanceResource = RdfHelper.getSubjectResource(response, RDF.type, ENEXA.ModuleInstance);
        if (instanceResource == null) {
            throw new Exception("Couldn't find module instance resource.");
        }
        return instanceResource.getURI();
    }

    /**
     * The main method of the SimpleClient class, responsible for initiating the Enexa experiment, adding files,
     * starting extraction, waiting for container running, retrieving metadata, and finding the result path.
     *
     * @param args the command-line arguments (not used)
     */
    public static void main(String[] args) {

        try (SimpleClient app = new SimpleClient()) {
            app.startExperiment();
            String moduleName = "extraction";
            String companyJsonIRI  = app.addFile("/home/farshad/test/enexa/shared/wikipedia_company_urls_short.json", moduleName);
            String generativeParameterIRI = app.addFile("/home/farshad/test/enexa/shared/generation_parameters.json", moduleName);
            String extractionInstanceIRI = app.startExtraction(companyJsonIRI, generativeParameterIRI);
            app.waitContainerRunning(extractionInstanceIRI);
            String metaDataEndPoint = app.getMeta();
            String resultPath=app.findResultPath(metaDataEndPoint, extractionInstanceIRI);
            System.out.println("result is saved at : "+ resultPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Finds the result path of the extraction instance from the metadata endpoint.
     *
     * @param metaDataEndPoint the metadata endpoint URL
     * @param instanceIRI      the IRI of the extraction instance
     * @return the path of the result file
     */
    private String findResultPath(String metaDataEndPoint, String instanceIRI) {

        DatasetDescription desc = new DatasetDescription();
        queryExecFactory = new QueryExecutionFactoryHttp(metaDataEndPoint, new DatasetDescription(), HttpClient.newHttpClient());
        queryExecFactory = new QueryExecutionFactoryPaginated(queryExecFactory, 10);


        String queryStr =  "SELECT ?path FROM <http://example.org/meta-data> WHERE {\n" +
            "  <"+instanceIRI+"> <http://w3id.org/dice-research/enexa/module/extraction/result/extractions_with_wikidata_triples> ?tmp.\n" +
            "\t ?tmp <http://w3id.org/dice-research/enexa/ontology#location> ?path.\n" +
            "}";
        // Create a SPARQL query object
        QueryExecution qe = queryExecFactory.createQueryExecution(queryStr);

        ResultSet rs = qe.execSelect();
        if (rs.hasNext()) {
            QuerySolution qs = rs.next();
            return qs.getLiteral("path").getString();
        } else {
            LOGGER.error("Couldn't get the expected result file from the meta data endpoint.");
        }
        return null;
    }

    /**
     * Retrieves the metadata endpoint URI based on the experiment IRI.
     *
     * @return the URI of the metadata endpoint
     */
    private String getMeta() {
        Model model =  requestGetRecieveModel(enexaURL + "/meta?experimentIRI="+experimentIRI);
        Resource expResource = RdfHelper.getObjectResource(model,null, ENEXA.metaDataEndpoint);
        return expResource.getURI();
    }

    /**
     * Waits until the container associated with the specified module instance IRI is running.
     *
     * @param instanceIRI the IRI of the module instance
     * @throws Exception if an error occurs while waiting for the container to run
     */
    private void waitContainerRunning(String instanceIRI) throws Exception {
        String body = " {\n" +
            "    \"moduleInstanceIRI\":\""+instanceIRI+"\",\n" +
            "    \"experimentIRI\":\""+experimentIRI+"\"\n" +
            "  }";
        boolean isRunning = true;
        String status = null;
        while (isRunning) {
            String response = requestPost(enexaURL + "/container-status", body);

            if (response == null) {
                throw new Exception("Couldn't get the status of a container.");
            }
            // Get the new IRI of the newly created module instance
            if (response.contains("run")) {
                Thread.sleep(1000);
            } else {
                return;
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (client != null) {
            client.close();
        }
    }
}
