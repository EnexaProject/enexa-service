/**
 * This file is part of core.
 *
 * core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with core.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.enexa.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.springframework.stereotype.Component;

/**
 * Representation of the ENEXA vocabulary as Java objects.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
@Component
public class ENEXA {

    protected static final String uri = "http://w3id.org/dice-research/enexa/ontology#";

    /**
     * returns the URI for this schema
     *
     * @return the URI for this schema
     */
    public static String getURI() {
        return uri;
    }

    protected static final Resource resource(String local) {
        return ResourceFactory.createResource(uri + local);
    }

    protected static final Property property(String local) {
        return ResourceFactory.createProperty(uri, local);
    }

    // Resources sorted alphabetically
    public static final Resource Module = resource("Module");
    public static final Resource ModuleInstance = resource("ModuleInstance");

    // Properties sorted alphabetically
    public static final Property containerName = property("containerName");
    public static final Property containerStatus = property("containerStatus");
    public static final Property experiment = property("experiment");
    public static final Property location = property("location");
    public static final Property metaDataEndpoint = property("metaDataEndpoint");
    public static final Property metaDataGraph = property("metaDataGraph");
    public static final Property moduleURL = property("moduleURL");

}
