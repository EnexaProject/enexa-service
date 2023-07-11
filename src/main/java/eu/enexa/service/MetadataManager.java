package eu.enexa.service;

import org.apache.jena.rdf.model.Model;

public interface MetadataManager {

    /**
     * Returns IRIs that are necessary to access an experiment's metadata. The first
     * String is the URL of the SPARQL endpoint while the second is the graph IRI in
     * which the metadata of the experiment can be found.
     *
     * @param experimentIri
     * @return
     */
    public String[] getMetadataEndpointInfo(String experimentIri);

    /**
     * Returns a random IRI that can be used within the metadata graph to name
     * resources.
     * 
     * @return a random IRI that is not yet present in the metadata graph.
     */
    public String generateResourceIRI();
    
    /**
     * Adds the given triples to the metadata graph.
     * 
     * @param model the triples that should be added to the metadata graph
     */
    public void addMetaData(Model model);
}
