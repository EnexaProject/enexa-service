package eu.enexa.service;

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

}
