package eu.enexa.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import eu.enexa.model.ModuleModel;
import eu.enexa.service.ModuleManager;
import eu.enexa.vocab.ENEXA;
import eu.enexa.vocab.HOBBIT;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.FileUtils;
import org.dice_research.rdf.RdfHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.type.UnknownTypeException;

public class FileBasedModuleManager implements ModuleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileBasedModuleManager.class);
    private Map<String, ModuleModel> modules;

    public FileBasedModuleManager() {
        //TODO : fill the map , do we need read it from file and write it after change ?
        this.modules = new HashMap<>();
    }

    @Override
    public ModuleModel deriveModule(String moduleIri, String moduleUrl) {
        // TODO : what are we going to do with moduleUrl
        if(modules.containsKey(moduleIri)) {
            return modules.get(moduleIri);
        } else {
            return null;
        }
    }

    public void addFileOrDirectory(File temp) throws FileNotFoundException {
        String extension = FileNameUtils.getExtension(temp.getName());
        if(extension.equals("ttl")){
            // read file
            Model model = ModelFactory.createDefaultModel();
            model.read(new FileInputStream(temp),"", FileUtils.langTurtle);

            ModuleModel moduleModel = new ModuleModel();

            Resource enexaModule = RdfHelper.getSubjectResource(model, null, ENEXA.Module);
            if(enexaModule == null){
                throw new IllegalArgumentException("File does not contains a "+ENEXA.Module.toString());
            }
            moduleModel.setModuleIri(enexaModule.getURI());

            //TODO : ask Micha if it is imageName or image
            Resource imageName = RdfHelper.getObjectResource(model, enexaModule, HOBBIT.image);
            if(imageName == null){
                throw new IllegalArgumentException("there is no image with this file ("+HOBBIT.image.toString()+")");
            }
            moduleModel.setImageName(imageName.getURI());

            //TODO : check if there is moduleURL in file add it , question how would be the triple for it ?

            modules.put(moduleModel.getModuleIri(),moduleModel);

            // save file in location
            //TODO : save file
        } else {
            LOGGER.error("the file in not known: " + temp.getName());
            throw new UnknownTypeException(null, temp);
        }
    }

}
