package eu.enexa.service;

import org.apache.jena.rdf.model.Model;

import java.util.List;

public interface MetadataManager {

    /**
     * Returns IRIs that are necessary to access an experiment's metadata. The first
     * String is the URL of the SPARQL endpoint while the second is the graph IRI in
     * which the metadata of the experiment can be found.
     *
     * @param experimentIri
     * @return
     */
    String[] getMetadataEndpointInfo(String experimentIri);

    /**
     * Returns a random IRI that can be used within the metadata graph to name
     * resources.
     *
     * @return a random IRI that is not yet present in the metadata graph.
     */
    String generateResourceIRI();

    /**
     * Adds the given triples to the metadata graph.
     *
     * @param model the triples that should be added to the metadata graph
     */
    void addMetaData(Model model);

    /**
     * Retrieve the container or pod name of the module instance with the given IRI
     * from the meta data graph.
     *
     * @param experimentIri the experiment's IRI in which the module instance is
     *                      involved
     * @param instanceIRI   the IRI of the instance for which the container name
     *                      should be retrieved
     * @return the container or pod name of the module instance with the given IRI
     */
    String getContainerName(String experimentIri, String instanceIRI);

    /**
     * Retrieve all the container or pod name of the given IRI
     * from the meta data graph.
     *
     * @param experimentIri the experiment's IRI in which the module instance is
     *                      involved
     * @return the container or pod name of the module instance with the given IRI
     */
    List<String> getAllContainerNames(String experimentIri);
}
