package eu.enexa.service;

import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EnexaServiceImpl implements EnexaService {

    @Autowired
    private ContainerManager containerManager;

    @Autowired
    private MetadataManager metadataManager;

    @Override
    public Model startExperiment() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Model getMetadataEndpoint(String experimentIri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Model startContainer(String experimentIri, String moduleIri, String moduleUrl,
            Map<String, String> parameters) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Model addResource(String experimentIri, String resource, String targetDir) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Model containerStatus(String experimentIri, String containerIri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Model stopContainer(String experimentIri, String containerIri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Model finishExperiment() {
        // TODO Auto-generated method stub
        return null;
    }

}
