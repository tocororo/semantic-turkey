package it.uniroma2.art.semanticturkey.customviews;

import org.eclipse.rdf4j.model.Value;

public class CustomViewObjectDescription {

    private Value resource;
    private Object description;


    /*
    TODO decide/restrict the type of the description
    Currently the description provides info for the client to inform how the value needs to be rendered
    - In sparql based views (e.g. maps and charts) it is a list of bindings
    - In single-value views (prop-chain or advanced) it is a single CustomViewRenderedValue
    - In vector views (static or dynamic) a list of CustomViewRenderedValue
     */

    public CustomViewObjectDescription() {}

    public CustomViewObjectDescription(Value object) {
        this.resource = object;
    }

    public Value getResource() {
        return resource;
    }

    public void setResource(Value resource) {
        this.resource = resource;
    }

    public Object getDescription() {
        return description;
    }

    public void setDescription(Object description) {
        this.description = description;
    }

}
