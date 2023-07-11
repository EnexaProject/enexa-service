package eu.enexa.sparql;

import java.util.UUID;

import org.aksw.jena_sparql_api.core.UpdateExecutionFactoryHttp;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.pagination.core.QueryExecutionFactoryPaginated;
import org.aksw.jenax.arq.connection.core.QueryExecutionFactory;
import org.aksw.jenax.arq.connection.core.UpdateExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateProcessor;
import org.dice_research.sparql.SparqlQueryUtils;
import org.springframework.stereotype.Component;

import eu.enexa.service.MetadataManager;

@Component
public class SparqlBasedMetadataManager implements MetadataManager, AutoCloseable {

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

    public SparqlBasedMetadataManager(String sparqlEndpointUrl) {
        this(sparqlEndpointUrl, DEFAULT_META_DATA_GRAPH_IRI, DEFAULT_RESOURCE_NAMESPACE);
    }

    public SparqlBasedMetadataManager(String sparqlEndpointUrl, String defaultMetaDataGraphIRI,
            String resourceNamespace) {
        this.sparqlEndpointUrl = sparqlEndpointUrl;
        this.defaultMetaDataGraphIRI = defaultMetaDataGraphIRI;
        this.resourceNamespace = resourceNamespace;

        queryExecFactory = new QueryExecutionFactoryHttp(sparqlEndpointUrl, defaultMetaDataGraphIRI);
        queryExecFactory = new QueryExecutionFactoryPaginated(queryExecFactory, DEFAULT_MAX_RESULT);
        updateExecFactory = new UpdateExecutionFactoryHttp(sparqlEndpointUrl);
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
        String[] queries = SparqlQueryUtils.getUpdateQueriesFromDiff(null, model, defaultMetaDataGraphIRI);
        UpdateProcessor update;
        for (String query : queries) {
            update = updateExecFactory.createUpdateProcessor(query);
            update.execute();
        }
    }

    @Override
    public void close() throws Exception {
        try {
            queryExecFactory.close();
        } catch (Exception e) {
        }
        try {
            updateExecFactory.close();
        } catch (Exception e) {
        }
    }

}
