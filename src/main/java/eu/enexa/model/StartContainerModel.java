package eu.enexa.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.HashMap;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class StartContainerModel {
    private String 	experiment;
    private String 	moduleIri;
    private String 	moduleUrl;
    private HashMap<String,String> parameters;

    public StartContainerModel(String experiment, String moduleIri, String moduleUrl, HashMap<String, String> parameters) {
        this.experiment = experiment;
        this.moduleIri = moduleIri;
        this.moduleUrl = moduleUrl;
        this.parameters = parameters;
    }
}
