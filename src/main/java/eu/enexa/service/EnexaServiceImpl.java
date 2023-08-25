package eu.enexa.service;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.rdf.ModelHelper;
import org.dice_research.rdf.RdfHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.enexa.model.AddedResource;
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

    private static final String metaDataEndpoint = "http://fuseki-devwd:3030/mydataset";

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
        // do not use experiment.getLocalName() it will remove the first character !

        sharedDirPath = sharedDirPath + File.separator + experiment.getURI().replace("http://", "");
        // TODO create directory
        File theDir = new File(sharedDirPath);
        if (!theDir.exists()) {
            boolean isCreated = theDir.mkdirs();
            if (!isCreated) {
                LOGGER.warn("the directory can not created at :" + sharedDirPath);
            }
        }
        // 3. Start default containers
        // TODO : implement this

        // 4. Update experiment meta data with data from steps 2 and 3
        model.add(experiment, RDF.type, ENEXA.Experiment);
        model.add(experiment, ENEXA.sharedDirectory, sharedDirPath);
        /*
         * The first String is the URL of the SPARQL endpoint while the second is the
         * graph IRI in which the metadata of the experiment can be found.
         */
        String[] metaDataInfos = metadataManager.getMetadataEndpointInfo(experimentIRI);
        if (metaDataInfos.length == 0) {
            LOGGER.error("there is no data in metadata for this experiments: " + experimentIRI);
        } else {
            Property sparqlEndpoint = ResourceFactory.createProperty(metaDataInfos[0]);
            model.add(experiment, ENEXA.metaDataEndpoint, sparqlEndpoint);

            if (metaDataInfos.length > 1) {
                // graphIRI
                // TODO : check if ENEXA.metaDataGraph is correct
                Property graphIRI = ResourceFactory.createProperty(metaDataInfos[1]);
                model.add(experiment, ENEXA.metaDataGraph, graphIRI);
            }
        }

        metadataManager.addMetaData(model);

        return model;
    }

    @Override
    public Model getMetadataEndpoint(String experimentIri) {
        String[] endpointInfo = metadataManager.getMetadataEndpointInfo(experimentIri);
        Model model = ModelFactory.createDefaultModel();
        Resource experimentResource = model.createResource(experimentIri);
        model.add(experimentResource, RDF.type, ENEXA.Experiment);
        model.add(experimentResource, ENEXA.metaDataEndpoint, model.createResource(endpointInfo[0]));
        model.add(experimentResource, ENEXA.metaDataGraph, model.createResource(endpointInfo[1]));
        return model;
    }

    @Override
    public Model startContainer(StartContainerModel scModel) throws RuntimeException {

        // TODO: establish exception package and move the exception to this place
        class ModuleNotFoundException extends RuntimeException {
            public ModuleNotFoundException() {
                super("Module with ID " + scModel.getModuleIri().toString() + " not found.");
            }
        }

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

        if (module == null) {
            throw new ModuleNotFoundException();
        }

        /*
         * 2. Create a resource for the new module instance in the meta data graph. Add
         * the parameters and their values.
         */
        String instanceIri = metadataManager.generateResourceIRI();
        scModel.setInstanceIri(instanceIri);
        metadataManager.addMetaData(scModel.getModel());

        Model tmpModel = ModelFactory.createDefaultModel();
        // tmpModel.addLiteral(tmpModel.createResource(instanceIri),tmpModel.createProperty("http://w3id.org/dice-research/enexa/module/dice-embeddings/parameters/num_epochs"),10);
        // metadataManager.addMetaData(tmpModel);
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
        /*
         * variables.add(new AbstractMap.SimpleEntry<>("ENEXA_META_DATA_ENDPOINT",
         * metadataManager.getMetadataEndpointInfo(scModel.getExperiment())[0]));
         */
        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_META_DATA_ENDPOINT", metaDataEndpoint));

        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_META_DATA_GRAPH",
                metadataManager.getMetadataEndpointInfo(scModel.getExperiment())[1]));
        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_MODULE_IRI", instanceIri));
        // TODO : after demo replace the hardcoded strings
        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_SHARED_DIRECTORY", "/enexa/"));
        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_WRITEABLE_DIRECTORY", "/enexa/"));

        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_MODULE_INSTANCE_IRI", scModel.getInstanceIri()));
        // TODO : should be specific
        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_MODULE_INSTANCE_DIRECTORY", "/output/result"));

        // TODO: update this
        if (System.getenv("ENEXA_SERVICE_URL").equals("")) {
            LOGGER.error("ENEXA_SERVICE_URL environment is null");
        } else {
            LOGGER.info("ENEXA_SERVICE_URL is : " + System.getenv("ENEXA_SERVICE_URL"));
        }
        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_SERVICE_URL", System.getenv("ENEXA_SERVICE_URL")));

        String containerId = containerManager.startContainer(module.getImage(), generatePodName(module.getModuleIri()),
                variables);
        // String containerId =
        // containerManager.startContainer("dicegroup/copaal-demo-service-splitedsearchcount:2.5.0",
        // generatePodName(module.getModuleIri()), variables);
        /*
         * 4. Add start time (or error code in case it couldn’t be started) to the TODO
         * create RDF model with new metadata metadataManager.addMetaData(null);
         */

        /*
         * 5. Return the meta data of the newly created container (including its DNS
         * name)
         */
        // TODO merge scModel and previously created metadata

        return scModel.toModel();
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
    public AddedResource addResource(Model requestModel) {
        Resource requestResIri = RdfHelper.getSubjectResource(requestModel, ENEXA.experiment, null);
        String newIri = metadataManager.generateResourceIRI();
        Resource newResource = requestModel.createResource(newIri);
        ModelHelper.replaceResource(requestModel, requestResIri, newResource);
        metadataManager.addMetaData(requestModel);
        return new AddedResource(newResource, requestModel);
    }

    @Override
    public AddedResource addResource(String experimentIri, String resource, String targetDir) {
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
