package eu.enexa.service.web;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;


import eu.enexa.vocab.ALGORITHM;
import eu.enexa.vocab.ENEXA;
import eu.enexa.vocab.HOBBIT;
import org.apache.jena.rdf.model.*;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import org.apache.jena.vocabulary.RDF;
import org.dice_research.rdf.RdfHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import eu.enexa.model.AddedResource;
import eu.enexa.model.StartContainerModel;
import eu.enexa.service.EnexaService;
import eu.enexa.vocab.ENEXA;
import eu.enexa.vocab.HOBBIT;

@RestController
public class EnexaController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnexaController.class);

    @Autowired
    private EnexaService enexa;

    @RequestMapping(value = "/add-resource", produces = { "application/json",
            "application/ld+json" }, method = RequestMethod.POST)
    public ResponseEntity<String> addResourceJsonLD(@RequestBody String body) {
        /*
         * Errors · HTTP 400: o Experiment IRI is not known / not available. o The
         * resource URL does not exist or cannot be downloaded. · HTTP 500: o An error
         * occurs while adding the resource.
         */
        Model request = readModel(body, "JSON-LD");
        if (request == null) {
            return new ResponseEntity<String>("Couldn't read provided RDF model.", HttpStatus.BAD_REQUEST);
        }
        // TODO : should all of these be null ?
        // Get RDF model from service as result of operation
        AddedResource addedResource = enexa.addResource(request);
        // serialize the model as JSON-LD
        String content = writeModel(addedResource.getModel(), "JSON-LD");
        ResponseEntity<String> entity;
        if (content == null) {
            entity = new ResponseEntity<String>("Couldn't serialize result model.", HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            entity = new ResponseEntity<String>(content, HttpStatus.OK);
        }
        // Add Content-Location header
        if ((addedResource.getResource() != null) && (addedResource.getResource().isURIResource())) {
            try {
                entity.getHeaders().add(HttpHeaders.CONTENT_LOCATION, addedResource.getResource().getURI());
            }catch(Exception ex){
                String error = ex.getMessage();
            }
        }
        return entity;
    }

    /*
     * @RequestMapping(value = "/add-resource", produces = { "application/xml",
     * "application/ld+json" }, method = RequestMethod.POST) public
     * ResponseEntity<String> addResourceXML() {
     *//*
        * Errors · HTTP 400: o Experiment IRI is not known / not available. o The
        * resource URL does not exist or cannot be downloaded. · HTTP 500: o An error
        * occurs while adding the resource.
        *//*
           * Model model = null; // Get RDF model from service as result of operation
           * String content = null; // serialize the model as RDF/XML return new
           * ResponseEntity<String>(content, HttpStatus.OK); }
           */

    @GetMapping("/container-status")
    public ResponseEntity<String> containerStatus(String experiment, String container) {
        /*
         * Errors · HTTP 400: o Experiment IRI is not known / not available. · HTTP 500:
         * o An error occurs while communicating with the Kubernetes service.
         */
        Model model = null; // Get RDF model from service as result of operation
        String content = null; // serialize the model as JSON-LD
        return new ResponseEntity<String>(content, HttpStatus.OK);
    }

    @RequestMapping(value = "/container-status", produces = { "application/json",
            "application/ld+json" }, method = RequestMethod.GET)
    public ResponseEntity<String> containerStatusJsonLD(@RequestBody String body) {
        /*
         * Errors · HTTP 400: o Experiment IRI is not known / not available. o The
         * resource URL does not exist or cannot be downloaded. · HTTP 500: o An error
         * occurs while adding the resource.
         */
        Model request = readModel(body, "JSON-LD");
        if (request == null) {
            return new ResponseEntity<String>("Couldn't read provided RDF model.", HttpStatus.BAD_REQUEST);
        }
        // TODO : should all of these be null ?
        // Get RDF model from service as result of operation
        Resource moduleInstance = RdfHelper.getSubjectResource(request, RDF.type, ENEXA.ModuleInstance);
        // TODO handle error
        RDFNode experiment = RdfHelper.getObjectResource(request, moduleInstance, ENEXA.experiment);
        // TODO handle error
        Model model = enexa.containerStatus(experiment.asResource().getURI(), moduleInstance.getURI());
        // serialize the model as JSON-LD
        String content = writeModel(model, "JSON-LD");
        if (content == null) {
            return new ResponseEntity<String>("Couldn't serialize result model.", HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            return new ResponseEntity<String>(content, HttpStatus.OK);
        }
    }

    @PostMapping("finish-experiment")
    public ResponseEntity<String> finishExperiment() {
        /*
         * Errors · HTTP 400: o Experiment IRI is not known / not available. · HTTP 500:
         * o An error occurs while communicating with the Kubernetes service.
         */
        Model model = null; // Get RDF model from service as result of operation
        String content = null; // serialize the model as JSON-LD
        return new ResponseEntity<String>(content, HttpStatus.OK);
    }

    // @PostMapping("start-container")
    // public ResponseEntity<String> startContainer(@RequestBody StartContainerModel
    // startContainerModel) {
    @PostMapping("start-container")
    public ResponseEntity<String> startContainer(@RequestBody String body) {
        /*
         * Errors · HTTP 400: o Experiment IRI is not known / not available. o The image
         * does not exist or cannot be found. · HTTP 500: o An error occurs while
         * communicating with the Kubernetes service.
         */
        Model model = ModelFactory.createDefaultModel();
        model.read(new StringReader(body), "", "JSON-LD");
        StartContainerModel scModel = StartContainerModel.parse(model);

        Model resultModel = enexa.startContainer(scModel);
        StringWriter writer = new StringWriter();
        resultModel.write(writer, "JSON-LD");
        return new ResponseEntity<String>(writer.toString(), HttpStatus.OK);
    }

    @PostMapping("start-experiment")
    public ResponseEntity<String> startExperiment() {
        /*
         * Errors · HTTP 400: o Experiment IRI is not known / not available. · HTTP 500:
         * o There is no such SPARQL endpoint available.
         *
         */

        Model model = enexa.startExperiment(); // Get RDF model from service as result of operation
        // serialize the model as JSON-LD
        StringWriter writer = new StringWriter();
        model.write(writer, "JSON-LD");
        return new ResponseEntity<String>(writer.toString(), HttpStatus.OK);
    }

    // This method finishes the experiment with the given IRI by stopping all its
    // remaining containers.
    @PostMapping("stop-container")
    public ResponseEntity<String> stopContainer(@RequestBody String body) {
        /*
         * Errors · HTTP 400: o Experiment IRI is not known / not available. o The image
         * does not exist or cannot be found. · HTTP 500: o An error occurs while
         * communicating with the Kubernetes service.
         */

        Model model = ModelFactory.createDefaultModel();
        model.read(new StringReader(body), "", "JSON-LD");

        StmtIterator iterator = model.listStatements(null, ALGORITHM.instanceOf, (RDFNode) null);
        if (!iterator.hasNext()) {
            throw new IllegalArgumentException("Couldn't find a module instance in the provided RDF model.");
        }

        Statement s = iterator.next();
        // If there is more than one hobbit:instanceOf triple
        if (iterator.hasNext()) {
            LOGGER.warn("Found multiple module instanceOf definitions. They will be ignored. Model dump: "
                    + model.toString());
        }

        // Get the instance representation
        Resource instance = s.getSubject();
        if (!s.getObject().isURIResource()) {
            throw new IllegalArgumentException("Got a module without an IRI.");
        }

        // Get the experiment IRI
        Resource experimentResource = RdfHelper.getObjectResource(model, instance, ENEXA.experiment);
        if ((experimentResource == null) || !experimentResource.isURIResource()) {
            throw new IllegalArgumentException("Got a Request without an experiment IRI.");
        }

        // Get RDF model from service as result of operation
        Model stopModel = enexa.stopContainer(experimentResource.getURI(), null);

        StringWriter writer = new StringWriter();
        stopModel.write(writer, "JSON-LD");
        return new ResponseEntity<String>(writer.toString(), HttpStatus.OK);
    }

    protected String writeModel(Model model, String language) {
        try (StringWriter writer = new StringWriter()) {
            model.write(writer, language);
            return writer.toString();
        } catch (IOException e) {
            LOGGER.error("Error while serializing model. Returning null.", e);
            return null;
        }
    }

    protected Model readModel(String body, String language) {
        // TODO We could also make use of the RDFDataMgr class here
        Model model = ModelFactory.createDefaultModel();
        try (StringReader reader = new StringReader(body)) {
            model.read(reader, "", language);
        } catch (Exception e) {
            LOGGER.error("Error while reading request model. Returning null.", e);
            return null;
        }
        return model;
    }

}
