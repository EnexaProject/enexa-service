package eu.enexa.service;

import org.apache.jena.rdf.model.Model;

import eu.enexa.model.StartContainerModel;

public interface EnexaService {

    /**
     * Start a new experiment.
     *
     *
     * 1. Generate experiment IRI and create its meta data 2. Create shared
     * directory 3. Start default containers 4. Update experiment meta data with
     * data from steps 2 and 3
     *
     * Response content: Experiment IRI, Meta data SPARQL endpoint URL, Shared
     * directory path
     *
     * @return
     */
    public Model startExperiment();

    /**
     * Return the metadata endpoint and graph IRI for the given experiment.
     *
     * @param experimentIri
     * @return
     */
    public Model getMetadataEndpoint(String experimentIri);

    /**
     *
     * 1. Derive meta data for the module that should be started a. The module IRI
     * is looked up in a local repository (e.g., in a set of files) or, b. The
     * module URL (if provided) is accessed via HTTP to load the module with the
     * given IRI; c. If the first two attempts are not possible or do not give any
     * result, the module IRI is dereferenced to download the meta data. If the data
     * contains more than one module, the “latest” (i.e., the one with the latest
     * publication date) is used. 2. Create a resource for the new module instance
     * in the meta data graph. Add the parameters and their values. 3. Start the
     * image a. with the ENEXA environmental variables b. as part of the local
     * network of the ENEXA service. 4. Add start time (or error code in case it
     * couldn’t be started) to the experiment’s meta data. 5. Return the meta data
     * of the newly created container (including its DNS name)
     *
     * @param scModel data object that contains all necessary information to start the container
     * @return
     */
    public Model startContainer(StartContainerModel scModel);

    /**
     * 1. If the resource has a known protocol that does not start with the file
     * protocol, the service should try to fetch the data. 2. Generate file name 3.
     * Add file to the shared directory 4. Add and return the file’s meta data
     *
     * @param experimentIri
     * @param resource
     * @param targetDir
     * @return Meta data of the newly added file
     */
    public Model addResource(String experimentIri, String resource, String targetDir);

    /**
     * This method returns status information for the given container that is
     * gathered from the Kubernetes service.
     *
     * @param experimentIri
     * @param containerIri
     * @return The status of the container expressed as RDF. This could also express
     *         that the container does not exist.
     */
    public Model containerStatus(String experimentIri, String containerIri);

    public Model stopContainer(String experimentIri, String containerIri);

    /**
     * 1. Iterate over the experiment’s container and stop them. 2. Update the
     * experiment meta data (e.g., the file location of a SPARQL endpoint’s content)
     *
     * @return
     */
    public Model finishExperiment();

}
