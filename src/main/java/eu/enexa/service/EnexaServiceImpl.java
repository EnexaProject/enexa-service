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
import org.dice_research.enexa.utils.EnexaPathUtils;
import org.dice_research.enexa.vocab.ENEXA;
import org.dice_research.rdf.ModelHelper;
import org.dice_research.rdf.RdfHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.enexa.model.AddedResource;
import eu.enexa.model.ModuleModel;
import eu.enexa.model.ModuleNotFoundException;
import eu.enexa.model.StartContainerModel;

@Service
public class EnexaServiceImpl implements EnexaService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnexaServiceImpl.class);
    private static final String DEFAULT_SHARED_DIRECTORY_FOR_RUNNING_CONTAINERS = "/home/shared";

    @Autowired
    private ContainerManager containerManager;

    @Autowired
    private MetadataManager metadataManager;

    @Autowired
    private ModuleManager moduleManager;

    private static final String metaDataEndpoint = System.getenv("ENEXA_META_DATA_ENDPOINT");
    private static final String appName = "app1";
    private static final String sharedDirectory = System.getenv("ENEXA_SHARED_DIRECTORY");
    //private static final String writeableDirectory = System.getenv("ENEXA_WRITEABLE_DIRECTORY");
    //private static final String moduleInstanceDirectory = System.getenv("ENEXA_MODULE_INSTANCE_DIRECTORY");

    @Override
    public Model startExperiment() {
        Model model = ModelFactory.createDefaultModel();
        // 1. Generate experiment IRI and create its meta data
        String experimentIRI = metadataManager.generateResourceIRI();
        Resource experiment = model.createResource(experimentIRI);

        // 2. Create shared directory
        String sharedDirLocalPath = System.getenv("ENEXA_SHARED_DIRECTORY");
        if (sharedDirLocalPath.endsWith(File.separator)) {
            sharedDirLocalPath = sharedDirLocalPath.substring(0, sharedDirLocalPath.length() - 1);
        }
        // do not use experiment.getLocalName() it will remove the first character !

        sharedDirLocalPath = sharedDirLocalPath +File.separator+appName+ File.separator + experiment.getURI().split("/")[experiment.getURI().split("/").length -1];
        // TODO create directory
          File theDir = new File(sharedDirLocalPath);
          if (!theDir.exists()){
              boolean isCreated = theDir.mkdirs();
              if(!isCreated){
                  LOGGER.warn("the directory can not created at :"+sharedDirLocalPath);
              }
          }
        // 3. Start default containers
        // TODO : implement this

        // 4. Update experiment meta data with data from steps 2 and 3
        model.add(experiment, RDF.type, ENEXA.Experiment);

        model.add(experiment, ENEXA.sharedDirectory,
                EnexaPathUtils.translateLocal2EnexaPath(sharedDirLocalPath, System.getenv("ENEXA_SHARED_DIRECTORY")));
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
    public Model startContainer(StartContainerModel scModel) throws ModuleNotFoundException {

        /*
         * 1. Derive meta data for the module that should be started a. The module IRI
         * is looked up in a local repository (e.g., in a set of files) or, b. The
         * module URL (if provided) is accessed via HTTP to load the module with the
         * given IRI; c. If the first two attempts are not possible or do not give any
         * result, the module IRI is dereferenced to download the meta data. If the data
         * contains more than one module, the "latest" (i.e., the one with the latest
         * publication date) is used.
         */
        ModuleModel module = null;
        try {
            module = moduleManager.deriveModule(scModel.getModuleIri(), scModel.getModuleUrl());
        } catch (Exception e) {
            throw new ModuleNotFoundException(scModel);
        }
        if (module == null) {
            throw new ModuleNotFoundException(scModel);
        }

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
        /*
         * variables.add(new AbstractMap.SimpleEntry<>("ENEXA_META_DATA_ENDPOINT",
         * metadataManager.getMetadataEndpointInfo(scModel.getExperiment())[0]));
         */
        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_META_DATA_ENDPOINT", metaDataEndpoint));

        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_META_DATA_GRAPH",
                metadataManager.getMetadataEndpointInfo(scModel.getExperiment())[1]));
        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_MODULE_IRI", instanceIri));

        //TODO : change the java util which in case of equal return enexa-dir:// not null
        // here is the shared directory of a starting container not host
        //variables.add(new AbstractMap.SimpleEntry<>("ENEXA_SHARED_DIRECTORY", EnexaPathUtils.translateLocal2EnexaPath(sharedDirectory, System.getenv("ENEXA_SHARED_DIRECTORY"))));
        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_SHARED_DIRECTORY", DEFAULT_SHARED_DIRECTORY_FOR_RUNNING_CONTAINERS));
        //variables.add(new AbstractMap.SimpleEntry<>("ENEXA_SHARED_DIRECTORY",  System.getenv("ENEXA_SHARED_DIRECTORY")));

        String appPath =sharedDirectory + File.separator + appName;
        if(sharedDirectory.endsWith(File.separator)){
            appPath = sharedDirectory+appName;
        }

        File appPathDirectory = new File(appPath);
        if(!appPathDirectory.exists()){
            appPathDirectory.mkdirs();
        }

        String writeableDirectory =scModel.getExperiment().split("/")[scModel.getExperiment().split("/").length - 1];

        String exprimentWriteablePath = appPath + File.separator + writeableDirectory;
        if(appPath.endsWith(File.separator)){
            exprimentWriteablePath = appPath+writeableDirectory;
        }

        File exprimentWriteablePathDirectory = new File(exprimentWriteablePath);
        if(!exprimentWriteablePathDirectory.exists()){
            exprimentWriteablePathDirectory.mkdirs();
        }

        //variables.add(new AbstractMap.SimpleEntry<>("ENEXA_WRITEABLE_DIRECTORY",DEFAULT_SHARED_DIRECTORY_FOR_RUNNING_CONTAINERS+File.separator+appName));
        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_WRITEABLE_DIRECTORY", exprimentWriteablePath));
        //private static final String writeableDirectory = System.getenv("ENEXA_WRITEABLE_DIRECTORY");
        //private static final String moduleInstanceDirectory = System.getenv("ENEXA_MODULE_INSTANCE_DIRECTORY");


        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_MODULE_INSTANCE_IRI", scModel.getInstanceIri()));


        //TODO : this should replace with better solution , maybe add module name to model to use it here
        String moduleInstanceDirectory =  scModel.getModuleIri().split("/")[scModel.getModuleIri().split("/").length - 2];

        String modulePath = exprimentWriteablePath + File.separator + moduleInstanceDirectory;
        if(exprimentWriteablePath.endsWith(File.separator)){
            modulePath = exprimentWriteablePath+moduleInstanceDirectory;
        }

        File modulePathDirectory = new File(modulePath);
        if(!modulePathDirectory.exists()){
            modulePathDirectory.mkdirs();
        }

        //variables.add(new AbstractMap.SimpleEntry<>("ENEXA_MODULE_INSTANCE_DIRECTORY",DEFAULT_SHARED_DIRECTORY_FOR_RUNNING_CONTAINERS+File.separator+appName+File.separator+moduleInstanceDirectory ));
        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_MODULE_INSTANCE_DIRECTORY", modulePath));


        // TODO: update this
        if (System.getenv("ENEXA_SERVICE_URL").equals("")) {
            LOGGER.error("ENEXA_SERVICE_URL environment is null");
        } else {
            LOGGER.info("ENEXA_SERVICE_URL is : " + System.getenv("ENEXA_SERVICE_URL"));
        }
        variables.add(new AbstractMap.SimpleEntry<>("ENEXA_SERVICE_URL", System.getenv("ENEXA_SERVICE_URL")));

        String containerName = generatePodName(module.getModuleIri());
        String containerId = containerManager.startContainer(module.getImage(), containerName, variables);
        // TODO take point in time

        /*
         * 4. Add start time (or error code in case it couldn’t be started) to the TODO
         * create RDF model with new metadata metadataManager.addMetaData(null);
         */
        Model createdContainerModel = scModel.getModel();
        Resource instanceRes = createdContainerModel.getResource(instanceIri);
        createdContainerModel.add(instanceRes, ENEXA.containerId, containerId);
        createdContainerModel.add(instanceRes, ENEXA.containerName, containerName);
        // TODO add start time

        metadataManager.addMetaData(createdContainerModel);

        /*
         * 5. Return the meta data of the newly created container (including its DNS
         * name)
         */
        return scModel.getModel();
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
        LOGGER.info("experimentIri is : "+ experimentIri);
        LOGGER.info("instanceIRI is : "+ instanceIRI);
        // Query container / pod name
        String podName = metadataManager.getContainerName(experimentIri, instanceIRI);
        LOGGER.info("pod/container name is : "+ podName);
        // Get status
        String status = containerManager.getContainerStatus(podName);
        LOGGER.info("container status is : "+ status);
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
