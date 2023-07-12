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

/**
 * Representation of the ENEXA vocabulary as Java objects.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
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
//    public static final Resource AnalysisAlgorithm = resource("AnalysisAlgorithm");
//    public static final Resource AnalysisResult = resource("AnalysisResult");
//    public static final Resource AnalysisResultset = resource("AnalysisResultset");
//    public static final Resource API = resource("API");
//    public static final Resource AscendingOrder = resource("AscendingOrder");
//    public static final Resource Benchmark = resource("Benchmark");
//    public static final Resource Challenge = resource("Challenge");
//    public static final Resource ChallengeTask = resource("ChallengeTask");
//    public static final Resource ConfigurableParameter = resource("ConfigurableParameter");
//    public static final Resource DescendingOrder = resource("DescendingOrder");
//    public static final Resource Error = resource("Error");
//    public static final Resource Experiment = resource("Experiment");
//    public static final Resource FeatureParameter = resource("FeatureParameter");
//    public static final Resource ForwardedParameter = resource("ForwardedParameter");
//    public static final Resource Hardware = resource("Hardware");
//    public static final Resource KPI = resource("KPI");
//    public static final Resource KPISeq = resource("KPISeq");
//    public static final Resource Parameter = resource("Parameter");
//    public static final Resource PearsonAlgorithm = resource("PearsonAlgorithm");
//    public static final Resource System = resource("System");
//    public static final Resource SystemInstance = resource("SystemInstance");

    // Properties sorted alphabetically
    public static final Property experiment = property("experiment");
    public static final Property instance = property("instance");
    public static final Property moduleURL = property("moduleURL");

}
