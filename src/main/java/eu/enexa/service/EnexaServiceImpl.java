package eu.enexa.service;

import eu.enexa.vocab.ENEXA;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import eu.enexa.model.ModuleModel;
import eu.enexa.model.StartContainerModel;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

@Service
public class EnexaServiceImpl implements EnexaService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnexaServiceImpl.class);

    @Autowired
    private ContainerManager containerManager;

    @Autowired
    private MetadataManager metadataManager;

    @Autowired
    private ModuleManager moduleManager;

    @Override
    public Model startExperiment() {
        //1.	Generate experiment IRI and create its meta data
        String experimentIRI = metadataManager.generateResourceIRI();

        //2.	Create shared directory
        String sharedDirPath = System.getenv("sharedDirectory");
        if (sharedDirPath.endsWith(File.separator)) {
            sharedDirPath = sharedDirPath.substring(0, sharedDirPath.length() - 1);
        }
        sharedDirPath = sharedDirPath +File.separator+"ex"+experimentIRI;

        //3.	Start default containers
        //TODO : implement this

        //4.	Update experiment meta data with data from steps 2 and 3
        Model model = ModelFactory.createDefaultModel();

        Resource instance = model.createResource(experimentIRI);
        model.add(instance, RDF.type, ENEXA.Experiment);
        model.add(instance, ENEXA.sharedDirectory, model.createResource(sharedDirPath));

        metadataManager.addMetaData(model);

        return model;
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
         * * experiment’s meta data.
         * ENEXA_EXPERIMENT_IRI StartContainerModel.experiment
           ENEXA_META_DATA_ENDPOINT metadataManager.getMetadataEndpointInfo()
           ENEXA_META_DATA_GRAPH //
           ENEXA_MODULE_IRI instanceIri
           ENEXA_SHARED_DIRECTORY  /enexa/ HARDCODED we should tell the container manager this is default mounting
           ENEXA_WRITEABLE_DIRECTORY  // for demo is same
           ENEXA_SERVICE_URL is it the Host (ourself) url  http://
         */


        List<AbstractMap.SimpleEntry<String, String>> variables = new ArrayList<>();
        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_EXPERIMENT_IRI", scModel.getExperiment()));
        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_META_DATA_ENDPOINT",
                metadataManager.getMetadataEndpointInfo(scModel.getExperiment())[0]));
        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_META_DATA_GRAPH",
                metadataManager.getMetadataEndpointInfo(scModel.getExperiment())[1]));
        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_MODULE_IRI", instanceIri));
        // TODO : after demo replace the hardcoded strings
        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_SHARED_DIRECTORY", "/enexa/"));
        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_WRITEABLE_DIRECTORY", "/enexa/"));

        //TODO: update this
        if(System.getenv("ENEXA_SERVICE_URL").equals("")){
            LOGGER.error("ENEXA_SERVICE_URL environment is null");
        }else{
            LOGGER.info("ENEXA_SERVICE_URL is : " + System.getenv("ENEXA_SERVICE_URL"));
        }
        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_SERVICE_URL", System.getenv("ENEXA_SERVICE_URL")));

        String containerId = containerManager.startContainer(module.getImage(), generatePodName(module.getModuleIri()),
                variables);
        /*
         * 4. Add start time (or error code in case it couldn’t be started) to the //
         * TODO create RDF model with new metadata metadataManager.addMetaData(null); /*
         * 5. Return the meta data of the newly created container (including its DNS
         * name)
         */
        // TODO merge scModel and previously created metadata
        return null;
    }

    public String generatePodName(String moduleIri) {
        /*
         * MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
         * messageDigest.update(moduleIri.getBytes()); String stringHash = new
         * String(messageDigest.digest());
         */
        return "enexa-" + Integer.toString(moduleIri.hashCode());
    }

    @Override
    public Model addResource(String experimentIri, String resource, String targetDir) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Model containerStatus(String experimentIri, String instanceIRI) {
        // Query container / pod name
        String podName = metadataManager.getContainerName(experimentIri, instanceIRI);
        // Get status
        String status = containerManager.getContainerStatus(podName);

        Model result = ModelFactory.createDefaultModel();
        Resource instance = result.createResource(instanceIRI);
        result.add(instance, ENEXA.experiment, result.createResource(experimentIri));
        result.add(instance, ENEXA.containerStatus, result.createLiteral(status));
        return result;
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
