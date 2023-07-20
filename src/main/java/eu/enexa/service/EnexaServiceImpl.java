package eu.enexa.service;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.rdf.ModelHelper;
import org.dice_research.rdf.RdfHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.enexa.model.ModuleModel;
import eu.enexa.model.StartContainerModel;
import eu.enexa.vocab.ENEXA;

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
        Model model = ModelFactory.createDefaultModel();
        // 1. Generate experiment IRI and create its meta data
        String experimentIRI = metadataManager.generateResourceIRI();
        Resource experiment = model.createResource(experimentIRI);

        // 2. Create shared directory
        String sharedDirPath = System.getenv("ENEXA_SHARED_DIRECTORY");
        if (sharedDirPath.endsWith(File.separator)) {
            sharedDirPath = sharedDirPath.substring(0, sharedDirPath.length() - 1);
        }
        sharedDirPath = sharedDirPath + File.separator + experiment.getLocalName();
        // TODO create directory

        // 3. Start default containers
        // TODO : implement this

        // 4. Update experiment meta data with data from steps 2 and 3
        model.add(experiment, RDF.type, ENEXA.Experiment);
        model.add(experiment, ENEXA.sharedDirectory, sharedDirPath);

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
         * the local network of the ENEXA service. * experiment’s meta data.
         * ENEXA_EXPERIMENT_IRI StartContainerModel.experiment ENEXA_META_DATA_ENDPOINT
         * metadataManager.getMetadataEndpointInfo() ENEXA_META_DATA_GRAPH //
         * ENEXA_MODULE_IRI instanceIri ENEXA_SHARED_DIRECTORY /enexa/ HARDCODED we
         * should tell the container manager this is default mounting
         * ENEXA_WRITEABLE_DIRECTORY // for demo is same ENEXA_SERVICE_URL is it the
         * Host (ourself) url http://
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

        // TODO: update this
        if (System.getenv("ENEXA_SERVICE_URL").equals("")) {
            LOGGER.error("ENEXA_SERVICE_URL environment is null");
        } else {
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
        return "enexa-" + Integer.toString(moduleIri.hashCode() * 31 + (int) (System.currentTimeMillis()));
    }

    @Override
    public Model addResource(Model requestModel) {
        Resource requestResIri = RdfHelper.getSubjectResource(requestModel, ENEXA.experiment, null);
        String newIri = metadataManager.generateResourceIRI();
        Resource newResource = requestModel.createResource(newIri);
        ModelHelper.replaceResource(requestModel, requestResIri, newResource);
        metadataManager.addMetaData(requestModel);
        return requestModel;
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
        Model model = ModelFactory.createDefaultModel();
        // finishes the experiment with the given IRI by stopping all its remaining
        // containers.

        // list of all containers
        // TODO : read from meta data or use labels ( we use meta data for now) get
        // module instance from it and also module instance lead to container name
        // updates and stores the meta data of the experiment in the shared directory
        return model;
    }

    @Override
    public Model finishExperiment() {
        // TODO Auto-generated method stub
        return null;
    }

}
