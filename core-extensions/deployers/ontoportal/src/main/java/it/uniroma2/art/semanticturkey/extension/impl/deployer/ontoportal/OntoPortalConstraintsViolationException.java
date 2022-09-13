package it.uniroma2.art.semanticturkey.extension.impl.deployer.ontoportal;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.uniroma2.art.semanticturkey.i18n.InternationalizedRuntimeException;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.services.ExceptionFacet;

import java.util.ArrayList;
import java.util.List;

public class OntoPortalConstraintsViolationException extends InternationalizedRuntimeException {
    public static class Repair {
        public String message;
        public PluginSpecification transformerSpecification;
    }
    public static class Violation {
        public String message;
        public List<Repair> fixes;
    }
    private List<Violation> violations;

    public OntoPortalConstraintsViolationException() {
        super(OntoPortalConstraintsViolationException.class.getName() + ".message", null);
        this.violations = new ArrayList<>();
    }

    public void addViolation(Violation v) {
        this.violations.add(v);
    }

    @ExceptionFacet("violations")
    public List<Violation> getViolations() {
        return violations;
    }
}
