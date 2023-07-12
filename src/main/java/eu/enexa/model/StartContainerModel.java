package eu.enexa.model;


import javax.annotation.Nullable;

import org.apache.jena.rdf.model.*;

import org.dice_research.rdf.RdfHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import eu.enexa.vocab.ENEXA;
import eu.enexa.vocab.HOBBIT;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the request to start an ENEXA module.
 *
 * @author TODO Farshad, please add yourself :D
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class StartContainerModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartContainerModel.class);

    /**
     * The experiment IRI to which the module belongs.
     */
    private String experiment;
    /**
     * The IRI of the module that should be instantiated.
     */
    private String moduleIri;
    /**
     * The URL at which the module's meta data can be found. This can be
     * {@code null}.
     */
    @Nullable
    private String moduleUrl;
    /**
     * <p>
     * The RDF data that came with the request. It may include additional
     * information about the parameters that are needed to run the module.
     * </p>
     *
     * <p>
     * Note that this class take ownership of this model and may change it.
     * </p>
     */
    private Model model;
    /**
     * The {@link Resource} in the given RDF {@link #model} that represents the
     * module instance that should be created.
     */
    private Resource instance;

    /**
     * Constructor.
     *
     * @param experiment The experiment IRI to which the module belongs.
     * @param moduleIri  The IRI of the module that should be instantiated.
     * @param moduleUrl  The URL at which the module's meta data can be found (can
     *                   be {@code null}).
     * @param instance   The Resource in the given model that represents the module
     *                   instance that should be created.
     * @param model      The RDF data that came with the request
     */
    public StartContainerModel(String experiment, String moduleIri, String moduleUrl, Resource instance, Model model) {
        this.experiment = experiment;
        this.moduleIri = moduleIri;
        this.moduleUrl = moduleUrl;
        this.model = model;
        this.instance = instance;
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
        Resource oldInstanceIRI = s.getSubject();

        // new resource
        Resource updatedInstanceIri = ResourceFactory.createResource(instanceIri);

        // get all triples with oldInstanceIRI
        StmtIterator oldInstanceIRIIterator = model.listStatements(oldInstanceIRI,(Property) null, (RDFNode) null);
        List<Statement> toRemove = new ArrayList<>();
        while(oldInstanceIRIIterator.hasNext()){
            // remove old one
            Statement oldStatement = oldInstanceIRIIterator.next();
            Property predicate = oldStatement.getPredicate();
            RDFNode object = oldStatement.getObject();
            toRemove.add(oldStatement);

            //add new one
            Statement newstatement  = model.createStatement(updatedInstanceIri, predicate, object);
            model.add(newstatement);
        }

        for (Statement statement:toRemove) {
            statement.remove();
        }

        this.instance = updatedInstanceIri;
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
                moduleUrlResource == null ? null : moduleUrlResource.getURI(), instance, model);
    }
}
