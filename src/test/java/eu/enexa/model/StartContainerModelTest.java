package eu.enexa.model;

import java.io.StringReader;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.dice_research.enexa.vocab.HOBBIT;
import org.dice_research.rdf.RdfHelper;
import org.dice_research.rdf.test.ModelComparisonHelper;
import org.junit.Assert;
import org.junit.Test;

public class StartContainerModelTest {

    @Test
    public void parseAndReplaceTest() {
        String requestString = "@prefix hobbit: <http://w3id.org/hobbit/vocab#> . "
                + "@prefix enexa:  <http://w3id.org/dice-research/enexa/ontology#> ."
                + "@prefix xsd:    <http://www.w3.org/2000/10/XMLSchema#> ."
                + " [] hobbit:instanceOf <http://dice-research.org/DICE-framework/v1.0> ;"
                + " enexa:experiment <http://example.org/experiment1> ; "
                + " <http://dice-research.org/DICE-framework/parameters/algorithm> <http://dice-research.org/DICE-framework/algorithms/ConEx> ;"
                + " <http://dice-research.org/DICE-framework/parameters/dimensions> \"25\"^^xsd:nonNegativeInteger ;"
                + " <http://dice-research.org/DICE-framework/parameters/knowledgeGraph> <http://example.org/experiment1/data/kg/dump.ttl> .";
        Model model = ModelFactory.createDefaultModel();
        model.read(new StringReader(requestString), "", "TURTLE");

        // Parse the model
        StartContainerModel scModel = StartContainerModel.parse(model);

        Assert.assertNotNull(scModel);
        Assert.assertEquals("http://example.org/experiment1", scModel.getExperiment());
        Assert.assertEquals("http://dice-research.org/DICE-framework/v1.0", scModel.getModuleIri());
        Assert.assertSame(model, scModel.getModel());

        Assert.assertNull(scModel.getInstanceIri());
        Assert.assertNull(scModel.getModuleUrl());

        // Set the instance IRI
        scModel.setInstanceIri("http://example.org/RandomIRI");

        String expectedString = "@prefix hobbit: <http://w3id.org/hobbit/vocab#> . "
                + "@prefix enexa:  <http://w3id.org/dice-research/enexa/ontology#> ."
                + "@prefix xsd:    <http://www.w3.org/2000/10/XMLSchema#> ."
                + " <http://example.org/RandomIRI> hobbit:instanceOf <http://dice-research.org/DICE-framework/v1.0> ;"
                + " enexa:experiment <http://example.org/experiment1> ; "
                + " <http://dice-research.org/DICE-framework/parameters/algorithm> <http://dice-research.org/DICE-framework/algorithms/ConEx> ;"
                + " <http://dice-research.org/DICE-framework/parameters/dimensions> \"25\"^^xsd:nonNegativeInteger ;"
                + " <http://dice-research.org/DICE-framework/parameters/knowledgeGraph> <http://example.org/experiment1/data/kg/dump.ttl> .";
        Model expectedModel = ModelFactory.createDefaultModel();

        expectedModel.read(new StringReader(expectedString), "", "TURTLE");

        ModelComparisonHelper.assertModelsEqual(expectedModel, model);
    }

    // in this test , call the replaceIRI several times and it should work
    @Test
    public void setInstanceIriWorks() {
        String requestString = "@prefix hobbit: <http://w3id.org/hobbit/vocab#> . "
                + "@prefix enexa:  <http://w3id.org/dice-research/enexa/ontology#> ."
                + "@prefix xsd:    <http://www.w3.org/2000/10/XMLSchema#> ."
                + " [] hobbit:instanceOf <http://dice-research.org/DICE-framework/v1.0> ;"
                + " enexa:experiment <http://example.org/experiment1> ; "
                + " <http://dice-research.org/DICE-framework/parameters/algorithm> <http://dice-research.org/DICE-framework/algorithms/ConEx> ;"
                + " <http://dice-research.org/DICE-framework/parameters/dimensions> \"25\"^^xsd:nonNegativeInteger ;"
                + " <http://dice-research.org/DICE-framework/parameters/knowledgeGraph> <http://example.org/experiment1/data/kg/dump.ttl> .";

        Model model = ModelFactory.createDefaultModel();
        model.read(new StringReader(requestString), "", "TURTLE");

        // Parse the model
        StartContainerModel scModel = StartContainerModel.parse(model);

        Model tmpModel = scModel.getModel();

        scModel.setInstanceIri("http://example.org/testIRI1");
        Resource actuallInstance = RdfHelper.getSubjectResource(tmpModel, HOBBIT.instanceOf,
                ResourceFactory.createResource("http://dice-research.org/DICE-framework/v1.0"));
        Assert.assertEquals(actuallInstance.getURI(), "http://example.org/testIRI1");

        scModel.setInstanceIri("http://example.org/zzz");
        actuallInstance = RdfHelper.getSubjectResource(tmpModel, HOBBIT.instanceOf,
                ResourceFactory.createResource("http://dice-research.org/DICE-framework/v1.0"));
        Assert.assertEquals(actuallInstance.getURI(), "http://example.org/zzz");

        scModel.setInstanceIri("http://example.org/aaa");
        actuallInstance = RdfHelper.getSubjectResource(tmpModel, HOBBIT.instanceOf,
                ResourceFactory.createResource("http://dice-research.org/DICE-framework/v1.0"));
        Assert.assertEquals(actuallInstance.getURI(), "http://example.org/aaa");

        String expectedString = "@prefix hobbit: <http://w3id.org/hobbit/vocab#> . "
                + "@prefix enexa:  <http://w3id.org/dice-research/enexa/ontology#> ."
                + "@prefix xsd:    <http://www.w3.org/2000/10/XMLSchema#> ."
                + " <http://example.org/aaa> hobbit:instanceOf <http://dice-research.org/DICE-framework/v1.0> ;"
                + " enexa:experiment <http://example.org/experiment1> ; "
                + " <http://dice-research.org/DICE-framework/parameters/algorithm> <http://dice-research.org/DICE-framework/algorithms/ConEx> ;"
                + " <http://dice-research.org/DICE-framework/parameters/dimensions> \"25\"^^xsd:nonNegativeInteger ;"
                + " <http://dice-research.org/DICE-framework/parameters/knowledgeGraph> <http://example.org/experiment1/data/kg/dump.ttl> .";

        Model expectedModel = ModelFactory.createDefaultModel();

        expectedModel.read(new StringReader(expectedString), "", "TURTLE");

        ModelComparisonHelper.assertModelsEqual(expectedModel, model);
    }

    public static void main(String[] args) {
        (new StartContainerModelTest()).parseAndReplaceTest();
    }
}
