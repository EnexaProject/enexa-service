package eu.enexa.sparql;

import eu.enexa.service.MetadataManager;

import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Component;

@Component
public class SparqlBasedMetadataManager implements MetadataManager {

    @Override
    public String[] getMetadataEndpointInfo(String experimentIri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String generateResourceIRI() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addMetaData(Model model) {
        // TODO Auto-generated method stub
        
    }

}
