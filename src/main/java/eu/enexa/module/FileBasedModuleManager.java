package eu.enexa.module;

import java.io.File;
import java.util.Map;

import eu.enexa.model.ModuleModel;
import eu.enexa.service.ModuleManager;

public class FileBasedModuleManager implements ModuleManager {

    private Map<String, ModuleModel> modules;
    
    @Override
    public ModuleModel deriveModule(String moduleIri, String moduleUrl) {
        if(modules.containsKey(moduleIri)) {
            return modules.get(moduleIri);
        } else {
            return null;
        }
    }

    public void addFileOrDirectory(File temp) {
        // TODO Auto-generated method stub
    }

}
