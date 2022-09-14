package it.uniroma2.art.semanticturkey.extension.extpts.deployer;

import it.uniroma2.art.semanticturkey.i18n.STMessageSource;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.services.ExceptionFacet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DeploymentConstraintsViolationException extends IOException {

    public static final String MESSAGE_KEY = DeploymentConstraintsViolationException.class.getName() + ".message";

    public static class Repair {
        public String message;
        public PluginSpecification transformerSpecification;
    }
    public static class Violation {
        public String message;
        public List<Repair> fixes;
    }
    private List<Violation> violations;

    public DeploymentConstraintsViolationException() {
        super(STMessageSource.getMessage(MESSAGE_KEY, Locale.ROOT), null);
        this.violations = new ArrayList<>();
    }

    public void addViolation(Violation v) {
        this.violations.add(v);
    }

    @ExceptionFacet("violations")
    public List<Violation> getViolations() {
        return violations;
    }

    @Override
    public String getLocalizedMessage() {
        return STMessageSource.getMessage(MESSAGE_KEY);
    }

}
