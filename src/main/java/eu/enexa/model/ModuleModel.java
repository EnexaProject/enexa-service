package eu.enexa.model;

import org.apache.jena.rdf.model.Model;

public class ModuleModel {

    private String moduleIri;
    private String moduleUrl;
    private String image;
    private Model model;

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
     * @return the image
     */
    public String getImage() {
        return image;
    }
    /**
     * @param image the image to set
     */
    public void setImage(String image) {
        this.image = image;
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

    @Override
    public String toString() {
        return "ModuleModel{" +
            "moduleIri='" + moduleIri + '\'' +
            ", moduleUrl='" + moduleUrl + '\'' +
            ", image='" + image + '\'' +
            ", model=" + model +
            '}';
    }
}
