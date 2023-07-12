package eu.enexa.service;

import eu.enexa.model.ModuleModel;
import org.springframework.stereotype.Component;



@Component
public interface ModuleManager {

    /*
     * 1. Derive meta data for the module that should be started
     * a. The module IRIis looked up in a local repository (e.g., in a set of files) or
     * b. The module URL (if provided) is accessed via HTTP to load the module with the
     * given IRI;
     * c. If the first two attempts are not possible or do not give any
     * result, the module IRI is dereferenced to download the meta data. If the data
     * contains more than one module, the "latest" (i.e., the one with the latest
     * publication date) is used.
     */
    ModuleModel deriveModule(String moduleIri, String moduleUrl);

}
