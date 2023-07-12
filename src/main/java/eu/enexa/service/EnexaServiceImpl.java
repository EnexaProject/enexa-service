package eu.enexa.service;

import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.enexa.model.ModuleModel;
import eu.enexa.model.StartContainerModel;

@Service
public class EnexaServiceImpl implements EnexaService {

    @Autowired
    private ContainerManager containerManager;

    @Autowired
    private MetadataManager metadataManager;

    @Autowired
    private ModuleManager moduleManager;

    @Override
    public Model startExperiment() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Model getMetadataEndpoint(String experimentIri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Model startContainer(StartContainerModel scModel) {
        /*
         * 1. Derive meta data for the module that should be started a. The module IRI
         * is looked up in a local repository (e.g., in a set of files) or, b. The
         * module URL (if provided) is accessed via HTTP to load the module with the
         * given IRI; c. If the first two attempts are not possible or do not give any
         * result, the module IRI is dereferenced to download the meta data. If the data
         * contains more than one module, the "latest" (i.e., the one with the latest
         * publication date) is used.
         */
        ModuleModel module = moduleManager.deriveModule(scModel.getModuleIri(), scModel.getModuleUrl());
        /*
         * 2. Create a resource for the new module instance in the meta data graph. Add
         * the parameters and their values.
         */
        String instanceIri = metadataManager.generateResourceIRI();
        scModel.setInstanceIri(instanceIri);
        metadataManager.addMetaData(scModel.getModel());
        /*
         * 3. Start the image a. with the ENEXA environmental variables b. as part of
         * the local network of the ENEXA service.
         */
        String containerId = containerManager.startContainer(module.getImage());
        /*
         * 4. Add start time (or error code in case it couldn’t be started) to the
         * experiment’s meta data.
         */
        // TODO create RDF model with new metadata
        metadataManager.addMetaData(null);
        /*
         * 5. Return the meta data of the newly created container (including its DNS
         * name)
         */
        // TODO merge scModel and previously created metadata
        return null;
    }

    @Override
    public Model addResource(String experimentIri, String resource, String targetDir) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Model containerStatus(String experimentIri, String containerIri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Model stopContainer(String experimentIri, String containerIri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Model finishExperiment() {
        // TODO Auto-generated method stub
        return null;
    }

}
