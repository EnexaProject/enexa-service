package eu.enexa.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.springframework.stereotype.Component;

/**
 * Representation of the ALGORITHM vocabulary as Java objects.
 *
 * @author Farshad Afshari (farshad.afshari@uni-paderborn.de)
 *
 */

@Component
public class ALGORITHM {
    protected static final String uri = "http://www.w3id.org/dice-research/ontologies/algorithm/2023/06/";

    /**
     * returns the URI for this schema
     *
     * @return the URI for this schema
     */
    public static String getURI() {
        return uri;
    }

    protected static final Resource resource(String local) {
        Resource tmp = ResourceFactory.createResource(uri + local);
        return tmp;

    }

    protected static final Property property(String local) {
        return ResourceFactory.createProperty(uri, local);
    }

    // Resources sorted alphabetically
    //public static final Resource Module = resource("Module");


    // Properties sorted alphabetically
    public static final Property instanceOf = property("instanceOf");
}
