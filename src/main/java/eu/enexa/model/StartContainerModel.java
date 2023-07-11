package eu.enexa.model;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.dice_research.rdf.RdfHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import eu.enexa.vocab.ENEXA;
import eu.enexa.vocab.HOBBIT;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class StartContainerModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartContainerModel.class);

    private String experiment;
    private String moduleIri;
    private String moduleUrl;
    private Model model;
    private Resource instance;

    public StartContainerModel(String experiment, String moduleIri, String moduleUrl, Model model) {
        this.experiment = experiment;
        this.moduleIri = moduleIri;
        this.moduleUrl = moduleUrl;
        this.model = model;
    }

    /**
     * @return the experiment
     */
    public String getExperiment() {
        return experiment;
    }

    /**
     * @param experiment the experiment to set
     */
    public void setExperiment(String experiment) {
        this.experiment = experiment;
    }

    /**
     * @return the moduleIri
     */
    public String getModuleIri() {
        return moduleIri;
    }

    /**
     * @param moduleIri the moduleIri to set
     */
    public void setModuleIri(String moduleIri) {
        this.moduleIri = moduleIri;
    }

    /**
     * @return the moduleUrl
     */
    public String getModuleUrl() {
        return moduleUrl;
    }

    /**
     * @param moduleUrl the moduleUrl to set
     */
    public void setModuleUrl(String moduleUrl) {
        this.moduleUrl = moduleUrl;
    }

    /**
     * @return the model
     */
    public Model getModel() {
        return model;
    }

    /**
     * @param model the model to set
     */
    public void setModel(Model model) {
        this.model = model;
    }

    /**
     * @return the instanceIri
     */
    public String getInstanceIri() {
        return instance.getURI();
    }

    /**
     * @param instanceIri the instanceIri to set
     */
    public void setInstanceIri(String instanceIri) {
        // TODO new resource
        // TODO replace old one in the model with new one
        // this.instance = instanceIri;
    }

    public static StartContainerModel parse(Model model) throws IllegalArgumentException {
        // Search for triple that uses hobbit:instanceOf
        StmtIterator iterator = model.listStatements(null, HOBBIT.instanceOf, (RDFNode) null);
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
        // Get the module IRI
        String moduleIri = s.getObject().asResource().getURI();

        // Get the experiment IRI
        Resource experimentResource = RdfHelper.getObjectResource(model, instance, ENEXA.experiment);
        if ((experimentResource == null) || !experimentResource.isURIResource()) {
            throw new IllegalArgumentException("Got a Request without an experiment IRI.");
        }

        // Get the module URL if it is available
        Resource moduleUrlResource = RdfHelper.getObjectResource(model, instance, ENEXA.moduleURL);

        return new StartContainerModel(experimentResource.getURI(), moduleIri,
                moduleUrlResource == null ? null : moduleUrlResource.getURI(), model);
    }
}
