package eu.enexa.sparql;

import java.net.http.HttpClient;
import java.util.*;

import org.aksw.jena_sparql_api.core.UpdateExecutionFactoryHttp;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.pagination.core.QueryExecutionFactoryPaginated;
import org.aksw.jenax.arq.connection.core.QueryExecutionFactory;
import org.aksw.jenax.arq.connection.core.UpdateExecutionFactory;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.DatasetDescription;
import org.apache.jena.update.UpdateProcessor;
import org.dice_research.sparql.SparqlQueryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.enexa.service.MetadataManager;

@Component
public class SparqlBasedMetadataManager implements MetadataManager, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparqlBasedMetadataManager.class);

    private static final String DEFAULT_META_DATA_GRAPH_IRI = "http://w3id.org/dice-research/enexa/meta1";
    private static final String DEFAULT_RESOURCE_NAMESPACE = "http://w3id.org/dice-research/enexa/default/";
    private static final int DEFAULT_MAX_RESULT = 200;

    /**
     * The URL of the SPARQL endpoint.
     */
    private String defaultMetaDataGraphIRI = null;

    /**
     * Namespace of newly created resources.
     */
    private String resourceNamespace = null;

    /**
     * The URL of the SPARQL endpoint.
     */
    private String sparqlEndpointUrl = null;

    /**
     * The Query factory used to query the SPARQL endpoint.
     */
    private QueryExecutionFactory queryExecFactory = null;

    /**
     * The Query factory used to query the SPARQL endpoint.
     */
    private UpdateExecutionFactory updateExecFactory = null;


    public SparqlBasedMetadataManager(@Value("${ENEXA_META_DATA_ENDPOINT}") String sparqlEndpointUrl,
            @Value("${ENEXA_META_DATA_GRAPH}") String defaultMetaDataGraphIRI,
            @Value("${ENEXA_RESOURCE_NAMESPACE}") String resourceNamespace) {
        this.sparqlEndpointUrl = sparqlEndpointUrl;
        this.defaultMetaDataGraphIRI = defaultMetaDataGraphIRI;
        this.resourceNamespace = resourceNamespace;
        LOGGER.info("initiate query execute");
        LOGGER.info(" sparqlEndpointUrl is: {}",sparqlEndpointUrl);
        LOGGER.info(" defaultMetaDataGraphIRI is{}: ",defaultMetaDataGraphIRI);
        LOGGER.info(" resourceNamespace is: {}",resourceNamespace);
        HttpClient client = HttpClient.newHttpClient();
        DatasetDescription desc = new DatasetDescription();
        desc.addNamedGraphURI(defaultMetaDataGraphIRI);
        queryExecFactory = new QueryExecutionFactoryHttp(sparqlEndpointUrl, new DatasetDescription(), client);
        queryExecFactory = new QueryExecutionFactoryPaginated(queryExecFactory, DEFAULT_MAX_RESULT);
        updateExecFactory = new UpdateExecutionFactoryHttp(sparqlEndpointUrl, desc ,client);
    }

    @Override
    public Map<String,String> getMetadataEndpointInfo() {
        Map<String,String> info = new HashMap<>();
        if(sparqlEndpointUrl == null) {
            LOGGER.warn("sparqlEndpointUrl is null");
        }
        if(defaultMetaDataGraphIRI == null) {
            LOGGER.warn("defaultMetaDataGraphIRI is null");
        }

        info.put("sparqlEndpointUrl",sparqlEndpointUrl);
        info.put("defaultMetaDataGraphIRI",defaultMetaDataGraphIRI);
        return info;
    }

    @Override
    public String generateResourceIRI() {
        return resourceNamespace + UUID.randomUUID();
    }

    @Override
    public void addMetaData(Model model) {
        try {
            String[] queries = SparqlQueryUtils.getUpdateQueriesFromDiff(null, model, defaultMetaDataGraphIRI);
            UpdateProcessor update;
            for (String query : queries) {
                try {
                    update = updateExecFactory.createUpdateProcessor(query);
                    update.execute();
                }catch (Exception ex){
                    LOGGER.error("Error executing query: {}", query, ex);
                }
            }
        }catch (Exception ex){
            LOGGER.error("error happened",ex);
        }
    }

    @Override
    public void close() throws Exception {
        try {
            queryExecFactory.close();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(),e);
        }
        try {
            updateExecFactory.close();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(),e);
        }
    }

    @Override
    public String getContainerName(String experimentIri, String instanceIRI) {
        //LOGGER.info("getting container name ");
        String query = "SELECT ?name FROM <"+defaultMetaDataGraphIRI+"> WHERE {" + "<" + instanceIRI
            + "> <http://w3id.org/dice-research/enexa/ontology#containerName> ?name }";
        LOGGER.info(" - query is: {}", query);
        QueryExecution qe = queryExecFactory.createQueryExecution(query);

        ResultSet rs = qe.execSelect();
        if (rs.hasNext()) {
            QuerySolution qs = rs.next();
            return qs.getLiteral("name").getString();
        } else {
            LOGGER.error("Couldn't get the expected result file from the meta data endpoint.");
        }
        return null;
    }

    @Override
    public List<String> getAllContainerNames(String experimentIri) {
        String query = "SELECT  ?name FROM <"+defaultMetaDataGraphIRI+"> WHERE {\n" +
            "  ?instanceIRI <http://w3id.org/dice-research/enexa/ontology#containerName> ?name .\n" +
            "  ?instanceIRI <http://w3id.org/dice-research/enexa/ontology#experiment> <"+experimentIri+"> .\n" +
            "}";
        QueryExecution qe = queryExecFactory.createQueryExecution(query);
        ResultSet rs = qe.execSelect();
        List<String> containerNames = new ArrayList<>();
        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            containerNames.add(qs.getLiteral("name").getString());
        }
        return containerNames;
    }

}
