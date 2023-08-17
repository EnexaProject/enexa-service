package eu.enexa.model;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

public class AddedResource {

    protected Resource resource;
    protected Model model;

    public AddedResource(Resource resource, Model model) {
        super();
        this.resource = resource;
        this.model = model;
    }

    /**
     * @return the resource
     */
    public Resource getResource() {
        return resource;
    }

    /**
     * @param resource the resource to set
     */
    public void setResource(Resource resource) {
        this.resource = resource;
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

}
