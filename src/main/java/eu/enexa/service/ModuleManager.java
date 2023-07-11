package eu.enexa.service;

import eu.enexa.model.ModuleModel;

public interface ModuleManager {

    ModuleModel deriveModule(String moduleIri, String moduleUrl);

}
