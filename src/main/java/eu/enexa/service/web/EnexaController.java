package eu.enexa.service.web;

import eu.enexa.model.StartContainerModel;

import java.io.StringReader;
import java.io.StringWriter;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import eu.enexa.service.EnexaService;

@RestController
public class EnexaController {

    @Autowired
    private EnexaService enexa;

    @RequestMapping(value = "/add-resource", produces = { "application/json",
            "application/ld+json" }, method = RequestMethod.POST)
    public ResponseEntity<String> addResourceJsonLD() {
        /*
         * Errors · HTTP 400: o Experiment IRI is not known / not available. o The
         * resource URL does not exist or cannot be downloaded. · HTTP 500: o An error
         * occurs while adding the resource.
         */
        Model model = null; // Get RDF model from service as result of operation
        String content = null; // serialize the model as JSON-LD
        return new ResponseEntity<String>(content, HttpStatus.OK);
    }

    @RequestMapping(value = "/add-resource", produces = { "application/xml",
            "application/ld+json" }, method = RequestMethod.POST)
    public ResponseEntity<String> addResourceXML() {
        /*
         * Errors · HTTP 400: o Experiment IRI is not known / not available. o The
         * resource URL does not exist or cannot be downloaded. · HTTP 500: o An error
         * occurs while adding the resource.
         */
        Model model = null; // Get RDF model from service as result of operation
        String content = null; // serialize the model as RDF/XML
        return new ResponseEntity<String>(content, HttpStatus.OK);
    }

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

    //@PostMapping("start-container")
    //public ResponseEntity<String> startContainer(@RequestBody StartContainerModel startContainerModel) {
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
        
        Model resultModel= enexa.startContainer(scModel);
        StringWriter writer = new StringWriter();
        resultModel.write(writer, "JSON-LD");
        return new ResponseEntity<String>(writer.toString(), HttpStatus.OK);
    }

    @PostMapping("start-experiment")
    public ResponseEntity<String> startExperiment() {
        /*
         * Errors · HTTP 400: o Experiment IRI is not known / not available. · HTTP 500:
         * o There is no such SPARQL endpoint available.
         */
        Model model = null; // Get RDF model from service as result of operation
        String content = null; // serialize the model as JSON-LD
        return new ResponseEntity<String>(content, HttpStatus.OK);
    }

    @PostMapping("stop-container")
    public ResponseEntity<String> stopContainer() {
        /*
         * Errors · HTTP 400: o Experiment IRI is not known / not available. o The image
         * does not exist or cannot be found. · HTTP 500: o An error occurs while
         * communicating with the Kubernetes service.
         */
        Model model = null; // Get RDF model from service as result of operation
        String content = null; // serialize the model as JSON-LD
        return new ResponseEntity<String>(content, HttpStatus.OK);
    }

}
