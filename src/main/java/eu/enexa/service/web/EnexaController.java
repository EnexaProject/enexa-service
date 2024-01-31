package eu.enexa.service.web;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import org.apache.hc.core5.http.HttpHeaders;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.WebContent;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.enexa.vocab.Algorithm;
import org.dice_research.enexa.vocab.ENEXA;
import org.dice_research.rdf.RdfHelper;
import org.dice_research.rdf.spring_jena.Jena2SpringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import eu.enexa.model.AddedResource;
import eu.enexa.model.ModuleNotFoundException;
import eu.enexa.model.StartContainerModel;
import eu.enexa.service.EnexaService;

@RestController
@RequestMapping(value = "/", consumes = { WebContent.contentTypeJSONLD, WebContent.contentTypeTurtle,
        WebContent.contentTypeTurtleAlt1, WebContent.contentTypeRDFXML, WebContent.contentTypeRDFJSON,
        WebContent.contentTypeTextPlain, WebContent.contentTypeNTriples, WebContent.contentTypeNTriplesAlt,
        WebContent.contentTypeXML, WebContent.contentTypeXMLAlt, WebContent.contentTypeTriG,
        WebContent.contentTypeNQuads, WebContent.contentTypeTriGAlt1, WebContent.contentTypeRDFProto,
        WebContent.contentTypeRDFThrift, WebContent.contentTypeNQuadsAlt1, WebContent.contentTypeTriX,
        WebContent.contentTypeTriXxml, WebContent.contentTypeN3, WebContent.contentTypeN3Alt1,
        WebContent.contentTypeN3Alt2 }, produces = { WebContent.contentTypeJSONLD, WebContent.contentTypeTurtle,
                WebContent.contentTypeTurtleAlt1, WebContent.contentTypeRDFXML, WebContent.contentTypeRDFJSON,
                WebContent.contentTypeTextPlain, WebContent.contentTypeNTriples, WebContent.contentTypeNTriplesAlt,
                WebContent.contentTypeXML, WebContent.contentTypeXMLAlt, WebContent.contentTypeTriG,
                WebContent.contentTypeNQuads, WebContent.contentTypeTriGAlt1, WebContent.contentTypeRDFProto,
                WebContent.contentTypeRDFThrift, WebContent.contentTypeNQuadsAlt1, WebContent.contentTypeTriX,
                WebContent.contentTypeTriXxml, WebContent.contentTypeN3, WebContent.contentTypeN3Alt1,
                WebContent.contentTypeN3Alt2 })
public class EnexaController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnexaController.class);

    protected final static Lang[] SUPPORTED_LANGUAGES = new Lang[] { Lang.JSONLD, Lang.TURTLE, Lang.NTRIPLES,
            Lang.RDFXML, Lang.TRIG, Lang.TRIX, Lang.NQUADS, Lang.RDFJSON, Lang.RDFPROTO, Lang.RDFTHRIFT, Lang.JSONLD10,
            Lang.JSONLD11 };
    public final static String[] SUPPORTED_MEDIA_TYPES = Jena2SpringUtils.SUPPORTED_MEDIA_TYPES;

    @Autowired
    private EnexaService enexa;

    @GetMapping(value = "/test")
    public ResponseEntity<String> test(){
        return new ResponseEntity<String>("OK!",HttpStatus.OK);
    }
    @PostMapping(value = "/add-resource")
    public ResponseEntity<Model> addResource(@RequestBody Model request) {
        /*
         * Errors · HTTP 400: o Experiment IRI is not known / not available. o The
         * resource URL does not exist or cannot be downloaded. · HTTP 500: o An error
         * occurs while adding the resource.
         */
        // Get RDF model from service as result of operation
        AddedResource addedResource = enexa.addResource(request);
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        // Add Content-Location header
        if ((addedResource.getResource() != null) && (addedResource.getResource().isURIResource())) {
            headers.add(HttpHeaders.CONTENT_LOCATION, addedResource.getResource().getURI());
        }
        return new ResponseEntity<Model>(addedResource.getModel(), headers, HttpStatus.OK);
    }

/*    @PostMapping(value = "/container-status", consumes = WebContent.contentTypeJSON)
    public ResponseEntity<Model> containerStatusFormData(
        @RequestBody Map<String, String> requestData) {
        *//*
         * Errors · HTTP 400: o Experiment IRI is not known / not available. o The
         * resource URL does not exist or cannot be downloaded. · HTTP 500: o An error
         * occurs while adding the resource.
         *//*
        String moduleInstanceIRI = requestData.get("moduleInstanceIRI");
        String experimentIRI = requestData.get("experimentIRI");
        Model model = enexa.containerStatus(experimentIRI, moduleInstanceIRI);
        return new ResponseEntity<Model>(model, HttpStatus.OK);
    }*/

    @PostMapping(value = "/container-status", consumes = WebContent.contentTypeJSON)
    public ResponseEntity<Model> containerStatusFormData(
        @RequestBody Map<String, String> requestData) {
        /*
         * Errors · HTTP 400: o Experiment IRI is not known / not available. o The
         * resource URL does not exist or cannot be downloaded. · HTTP 500: o An error
         * occurs while adding the resource.
         */
        String moduleInstanceIRI = requestData.get("moduleInstanceIRI");
        String experimentIRI = requestData.get("experimentIRI");
        Model model = enexa.containerStatus(experimentIRI, moduleInstanceIRI);
        return new ResponseEntity<Model>(model, HttpStatus.OK);
    }

    @GetMapping(value = "/container-status")
    public ResponseEntity<Model> containerStatusRDF(@RequestBody Model request) {
        /*
         * Errors · HTTP 400: o Experiment IRI is not known / not available. o The
         * resource URL does not exist or cannot be downloaded. · HTTP 500: o An error
         * occurs while adding the resource.
         */
        Resource moduleInstance = RdfHelper.getSubjectResource(request, RDF.type, ENEXA.ModuleInstance);
        if ((moduleInstance == null) || (!moduleInstance.isURIResource())) {
            LOGGER.warn("Request model did not contain a module instance IRI. Returning HTTP 400. Model: "
                    + request.toString());
            return new ResponseEntity<Model>((Model) null, HttpStatus.BAD_REQUEST);
        }
        Resource experiment = RdfHelper.getObjectResource(request, moduleInstance, ENEXA.experiment);
        if ((experiment == null) || (!experiment.isURIResource())) {
            LOGGER.warn("Request model did not contain an experiment IRI. Returning HTTP 400. Model: "
                    + request.toString());
            return new ResponseEntity<Model>((Model) null, HttpStatus.BAD_REQUEST);
        }
        Model model = enexa.containerStatus(experiment.getURI(), moduleInstance.getURI());
        return new ResponseEntity<Model>(model, HttpStatus.OK);
    }

    @PostMapping("/finish-experiment")
    public ResponseEntity<String> finishExperiment() {
        /*
         * Errors · HTTP 400: o Experiment IRI is not known / not available. · HTTP 500:
         * o An error occurs while communicating with the Kubernetes service.
         */
//        Model model = null; // Get RDF model from service as result of operation
        String content = null; // serialize the model as JSON-LD
        return new ResponseEntity<String>(content, HttpStatus.OK);
    }

    @PostMapping(value = "/start-container")
    public ResponseEntity<Model> startContainer(@RequestBody Model request) {
        /*
         * Errors · HTTP 400: o Experiment IRI is not known / not available. o The image
         * does not exist or cannot be found. · HTTP 500: o An error occurs while
         * communicating with the Kubernetes service.
         */
        StartContainerModel scModel = StartContainerModel.parse(request);
        Model resultModel = null;
        try {
            resultModel = enexa.startContainer(scModel);
        } catch (ModuleNotFoundException e) {
            LOGGER.error("Requested module couldn't be started.", e);
            return new ResponseEntity<Model>((Model) null, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<Model>(resultModel, HttpStatus.OK);
    }

    @PostMapping(value = "/start-experiment")
    public Model startExperiment() {
        /*
         * HTTP 500: There is no such SPARQL endpoint available.
         */
        Model model = enexa.startExperiment(); // Get RDF model from service as result of operation
        return model;
    }

    // This method finishes the experiment with the given IRI by stopping all its
    // remaining containers.
    @PostMapping(value = "/stop-container")
    public ResponseEntity<Model> stopContainer(@RequestBody Model request) {
        /*
         * Errors · HTTP 400: o Experiment IRI is not known / not available. o The image
         * does not exist or cannot be found. · HTTP 500: o An error occurs while
         * communicating with the Kubernetes service.
         */

        // Get the instance representation
        Resource instance = RdfHelper.getSubjectResource(request, RDF.type, null);
        if (instance == null || !instance.isURIResource()) {
            throw new IllegalArgumentException("Got a module without an IRI.");
        }

        // Get the experiment IRI
        Resource experimentResource = RdfHelper.getObjectResource(request, instance, ENEXA.experiment);
        if ((experimentResource == null) || !experimentResource.isURIResource()) {
            throw new IllegalArgumentException("Got a Request without an experiment IRI.");
        }

        String containerIRI = RdfHelper.getLiteral(request, instance, ENEXA.containerId).toString();

        // Get RDF model from service as result of operation
        Model stopModel = enexa.stopContainer(experimentResource.getURI(), containerIRI);

        return new ResponseEntity<Model>(stopModel, HttpStatus.OK);
    }


    @GetMapping(value = "/meta")
    public ResponseEntity<Model> meta(@RequestParam String experimentIRI) {
        /*
         * Errors · HTTP 400: o Experiment IRI is not known / not available. o The
         * resource URL does not exist or cannot be downloaded. · HTTP 500: o An error
         * occurs while adding the resource.
         */
//        Resource experiment = RdfHelper.getSubjectResource(request, RDF.type, ENEXA.Experiment);
//        if (experiment == null) {
//            return new ResponseEntity<Model>((Model) null, HttpStatus.BAD_REQUEST);
//        }
        // Get RDF model from service as result of operation
        Model metadata = enexa.getMetadataEndpoint(experimentIRI);
        if (metadata == null) {
            return new ResponseEntity<Model>((Model) null, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<Model>(metadata, HttpStatus.OK);
    }

    @Deprecated
    protected String writeModel(Model model, String language) {
        try (StringWriter writer = new StringWriter()) {
            model.write(writer, language);
            return writer.toString();
        } catch (IOException e) {
            LOGGER.error("Error while serializing model. Returning null.", e);
            return null;
        }
    }

    @Deprecated
    protected Model readModel(String body, String contentType) {
        Lang lang = Lang.JSONLD;
        if (contentType != null) {
            ContentType ct = WebContent.determineCT(contentType, null, null);
            lang = RDFLanguages.contentTypeToLang(ct);
            if (lang == null) {
                LOGGER.warn("Got an unknown content type \"{}\". Using default RDF serialization.");
                lang = Lang.JSONLD;
            }
        }
        Model model = ModelFactory.createDefaultModel();
        try (StringReader reader = new StringReader(body)) {
            RDFDataMgr.read(model, reader, "", lang);
        } catch (Exception e) {
            LOGGER.error("Error while reading request model. Returning null.", e);
            return null;
        }
        return model;
    }

}
