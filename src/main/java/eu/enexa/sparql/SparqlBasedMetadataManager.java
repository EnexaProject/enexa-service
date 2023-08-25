package eu.enexa.sparql;

import java.net.http.HttpClient;
import java.util.UUID;

import org.aksw.jena_sparql_api.core.UpdateExecutionFactoryHttp;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.pagination.core.QueryExecutionFactoryPaginated;
import org.aksw.jenax.arq.connection.core.QueryExecutionFactory;
import org.aksw.jenax.arq.connection.core.UpdateExecutionFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
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

//    public SparqlBasedMetadataManager(@Value("${ENEXA_META_DATA_ENDPOINT}") String sparqlEndpointUrl) {
//        this(sparqlEndpointUrl, DEFAULT_META_DATA_GRAPH_IRI, DEFAULT_RESOURCE_NAMESPACE);
//    }

    public SparqlBasedMetadataManager(@Value("${ENEXA_META_DATA_ENDPOINT}") String sparqlEndpointUrl,
            @Value("${ENEXA_META_DATA_GRAPH}") String defaultMetaDataGraphIRI,
            @Value("${ENEXA_RESOURCE_NAMESPACE}") String resourceNamespace) {
        this.sparqlEndpointUrl = sparqlEndpointUrl;
        this.defaultMetaDataGraphIRI = defaultMetaDataGraphIRI;
        this.resourceNamespace = resourceNamespace;

        HttpClient client = HttpClient.newHttpClient();
        DatasetDescription desc = new DatasetDescription();
        desc.addNamedGraphURI(defaultMetaDataGraphIRI);
        queryExecFactory = new QueryExecutionFactoryHttp(sparqlEndpointUrl, new DatasetDescription(), client);
        queryExecFactory = new QueryExecutionFactoryPaginated(queryExecFactory, DEFAULT_MAX_RESULT);
        updateExecFactory = new UpdateExecutionFactoryHttp(sparqlEndpointUrl, desc ,client);
    }

    @Override
    public String[] getMetadataEndpointInfo(String experimentIri) {
        return new String[] { sparqlEndpointUrl, defaultMetaDataGraphIRI };
    }

    @Override
    public String generateResourceIRI() {
        String resourceIri = resourceNamespace + UUID.randomUUID().toString();
        return resourceIri;
    }

    @Override
    public void addMetaData(Model model) {
        try {
            String[] queries = SparqlQueryUtils.getUpdateQueriesFromDiff(null, model, defaultMetaDataGraphIRI);
            UpdateProcessor update;
            for (String query : queries) {
                //TODO if work should not be hardcoded
                //query = query.replace("WITH <mydataset>","");
                LOGGER.info("query is :"+query);
                //query = query.replace("INSERT","INSERT DATA").replace("WHERE","").replace("{}","");
                //LOGGER.info("after replacing the query where clause :"+query);
                update = updateExecFactory.createUpdateProcessor(query);
                update.execute();
                //todo what happened if update can not find a triple to update !
            }
        }catch (Exception ex){
            LOGGER.error(ex.getMessage());
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
        QueryExecution qe = queryExecFactory.createQueryExecution("SELECT ?name FROM <"+defaultMetaDataGraphIRI+"> WHERE {" + "<" + instanceIRI
                + "> <http://w3id.org/dice-research/enexa/ontology#containerName> ?name }");
        ResultSet rs = qe.execSelect();
        if (rs.hasNext()) {
            QuerySolution qs = rs.next();
            return qs.getLiteral("name").getString();
        } else {
            LOGGER.error("Couldn't get the expected result file from the meta data endpoint.");
        }
        return null;
    }

}
