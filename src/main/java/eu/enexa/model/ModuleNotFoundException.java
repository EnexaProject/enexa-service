package eu.enexa.model;

public class ModuleNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    public ModuleNotFoundException(StartContainerModel scModel) {
        super("Module with IRI " + scModel.getModuleIri() + " could not be found.");
    }

    public ModuleNotFoundException(StartContainerModel scModel, Exception cause) {
        super("Module with IRI " + scModel.getModuleIri() + " could not be found.", cause);
    }

}
