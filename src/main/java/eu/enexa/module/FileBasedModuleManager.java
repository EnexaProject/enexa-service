package eu.enexa.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileUtils;
import org.dice_research.rdf.RdfHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.enexa.model.ModuleModel;
import eu.enexa.service.ModuleManager;
import eu.enexa.vocab.ENEXA;
import eu.enexa.vocab.HOBBIT;

public class FileBasedModuleManager implements ModuleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileBasedModuleManager.class);

    private boolean throwIfFileUnknown = true;
    private boolean throwIfMissingInformation = true;
    private Map<String, ModuleModel> modules;

    public FileBasedModuleManager() {
        // TODO : fill the map , do we need read it from file and write it after change
        // ?
        this.modules = new HashMap<>();
    }

    @Override
    public ModuleModel deriveModule(String moduleIri, String moduleUrl) {
        // Lookup the model based on its IRI. The URL is ignored in this implementation
        // TODO possible extension: check whether the URL is a File URL; if that is the
        // case, read the module from this file.
        if (modules.containsKey(moduleIri)) {
            return modules.get(moduleIri);
        } else {
            return null;
        }
    }

    public void addFileOrDirectory(File moduleFile) throws IOException {
        if (!moduleFile.exists()) {
            throw new FileNotFoundException("There is not file \"" + moduleFile.getAbsolutePath() + "\".");
        }
        // If it is a directory, iterate over its content.
        if (moduleFile.isDirectory()) {
            for (File file : moduleFile.listFiles()) {
                addFileOrDirectory(file);
            }
        } else {
            String extension = FileNameUtils.getExtension(moduleFile.getName());
            if (extension.equals("ttl")) {
                // read file
                Model model = ModelFactory.createDefaultModel();
                model.read(new FileInputStream(moduleFile), "", FileUtils.langTurtle);

                // Find the resource representing the module
                Resource enexaModule = RdfHelper.getSubjectResource(model, null, ENEXA.Module);
                if (enexaModule == null) {
                    if (throwIfMissingInformation) {
                        throw new IllegalArgumentException(
                                "File does not contain an instance of " + ENEXA.Module.toString());
                    } else {
                        LOGGER.warn("The file \"{}\" does not contain an instance of {}. It will be ignored.",
                                moduleFile.getAbsolutePath(), ENEXA.Module.toString());
                    }
                }

                // Get the image
                Resource image = RdfHelper.getObjectResource(model, enexaModule, HOBBIT.image);
                if (image == null) {
                    if (throwIfMissingInformation) {
                        throw new IllegalArgumentException(
                                "There is no image for the module with the IRI " + enexaModule.toString() + ".");
                    } else {
                        LOGGER.warn("The module {} from file \"{}\" does not define an image. It will be ignored.",
                                enexaModule.toString(), moduleFile.getAbsolutePath());
                    }
                }
                if (!image.isURIResource()) {
                    if (throwIfMissingInformation) {
                        throw new IllegalArgumentException(
                                "The image for the module " + enexaModule.toString() + " is not an IRI.");
                    } else {
                        LOGGER.warn("The module {} from file \"{}\" does not define an image. It will be ignored.",
                                enexaModule.toString(), moduleFile.getAbsolutePath());
                    }
                }

                // create the module representation and add the values
                ModuleModel moduleModel = new ModuleModel();
                moduleModel.setModel(model);
                moduleModel.setModuleIri(enexaModule.getURI());
                moduleModel.setImage(image.getURI());

                // Add it to the internal index
                modules.put(moduleModel.getModuleIri(), moduleModel);
            } else {
                if (throwIfFileUnknown) {
                    throw new IOException(
                            "The given file has an unknown file ending: \"" + moduleFile.getAbsolutePath() + "\".");
                } else {
                    LOGGER.warn("The given file has an unknown file ending: \"" + moduleFile.getAbsolutePath()
                            + "\". It will be ignored.");
                }
            }
        }
    }

    /**
     * @param throwIfFileUnknown the throwIfFileUnknown to set
     */
    public void setThrowIfFileUnknown(boolean throwIfFileUnknown) {
        this.throwIfFileUnknown = throwIfFileUnknown;
    }

    /**
     * @param throwIfMissingInformation the throwIfMissingInformation to set
     */
    public void setThrowIfMissingInformation(boolean throwIfMissingInformation) {
        this.throwIfMissingInformation = throwIfMissingInformation;
    }

    /**
     * @return the modules
     */
    public Map<String, ModuleModel> getModules() {
        return modules;
    }

    /**
     * @param modules the modules to set
     */
    public void setModules(Map<String, ModuleModel> modules) {
        this.modules = modules;
    }

}
