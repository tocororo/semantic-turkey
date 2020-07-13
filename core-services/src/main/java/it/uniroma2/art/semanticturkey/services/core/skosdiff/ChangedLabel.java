package it.uniroma2.art.semanticturkey.services.core.skosdiff;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangedLabel {
    //all the parameter are string of the form NTriplesUtil.toNTriplesString

    private String label;

    private String resource; // if this is not null, then addedResource and removedResouce must be null
    private String addedResource;
    private String removedResouce;

    private String literalForm; // if this is not null, then addedLiteralForm and removedLiteralForm must be null
    private String addedLiteralForm;
    private String removedLiteralForm;

    //the list of String are a list of Value, and are a List of NTriplesUtil.toNTriplesString(Value)
    private Map<String, List<String>> propToRemovedValueListMap = new HashMap<>(); //the prop is in <propIRI>
    private Map<String, List<String>> propToAddedValueListMap = new HashMap<>(); //the prop is in <propIRI>

    //the list of String, are a list of Literal in the form "text"@LANG
    private Map<String, List<String>> notePropToRemovedNoteValueListMap = new HashMap<>(); //the prop is in <propIRI>
    private Map<String, List<String>> notePropToAddedNoteValueListMap = new HashMap<>(); //the prop is in <propIRI>


    public ChangedLabel() {
    }

    public ChangedLabel(IRI label) {
        this.label = NTriplesUtil.toNTriplesString(label);
    }

    public String getLabel() {
        return label;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(IRI resource) {
        this.resource = NTriplesUtil.toNTriplesString(resource);
    }

    public String getAddedResource() {
        return addedResource;
    }

    public void setAddedResource(IRI addedResource) {
        this.addedResource = NTriplesUtil.toNTriplesString(addedResource);
    }

    public String getRemovedResouce() {
        return removedResouce;
    }

    public void setRemovedResouce(IRI removedResouce) {
        this.removedResouce = NTriplesUtil.toNTriplesString(removedResouce);
    }

    public String getLiteralForm() {
        return literalForm;
    }

    public void setLiteralForm(Literal literalForm) {
        this.literalForm = NTriplesUtil.toNTriplesString(literalForm);
    }

    public String getAddedLiteralForm() {
        return addedLiteralForm;
    }

    public void setAddedLiteralForm(Literal addedLiteralForm) {
        this.addedLiteralForm = NTriplesUtil.toNTriplesString(addedLiteralForm);
    }

    public String getRemovedLiteralForm() {
        return removedLiteralForm;
    }

    public void setRemovedLiteralForm(Literal removedLiteralForm) {
        this.removedLiteralForm = NTriplesUtil.toNTriplesString(removedLiteralForm);
    }

    public Map<String, List<String>> getPropToRemovedValueListMap() {
        return propToRemovedValueListMap;
    }

    public void addPropToRemovedValue(IRI prop, Value value){
        if(prop == null || value == null){
            return;
        }
        String propString = NTriplesUtil.toNTriplesString(prop);
        if(value instanceof BNode){
            //do not add BNode
            return;
        }
        if(!propToRemovedValueListMap.containsKey(propString)){
            propToRemovedValueListMap.put(propString, new ArrayList<>());
        }
        //iterate to check if the value is already present
        for(String existingValue : propToRemovedValueListMap.get(propString)){
            if(existingValue.equals(NTriplesUtil.toNTriplesString(value))){
                //the value is already present, so just return
                return;
            }
        }
        propToRemovedValueListMap.get(propString).add(NTriplesUtil.toNTriplesString(value));
    }

    public Map<String, List<String>> getPropToAddedValueListMap() {
        return propToAddedValueListMap;
    }

    public void addPropToAddedValue(IRI prop, Value value){
        if(prop == null || value == null){
            return;
        }
        String propString = NTriplesUtil.toNTriplesString(prop);
        if(value instanceof BNode){
            //do not add BNode
            return;
        }
        if(!propToAddedValueListMap.containsKey(propString)){
            propToAddedValueListMap.put(propString, new ArrayList<>());
        }
        //iterate to check if the value is already present
        for(String existingValue : propToAddedValueListMap.get(propString)){
            if(existingValue.equals(NTriplesUtil.toNTriplesString(value))){
                //the value is already present, so just return
                return;
            }
        }
        propToAddedValueListMap.get(propString).add(NTriplesUtil.toNTriplesString(value));
    }

    public Map<String, List<String>> getNotePropToRemovedNoteValueListMap() {
        return notePropToRemovedNoteValueListMap;
    }

    public void addNotePropAndRemovedNoteValue(IRI noteProp, Literal noteValue){
        if(noteProp == null || noteValue == null){
            return;
        }
        String propString = NTriplesUtil.toNTriplesString(noteProp);
        if(!notePropToRemovedNoteValueListMap.containsKey(propString)){
            notePropToRemovedNoteValueListMap.put(propString, new ArrayList<>());
        }
        //iterate to check if the value is already present
        for(String existingValue : notePropToRemovedNoteValueListMap.get(propString)){
            if(existingValue.equals(NTriplesUtil.toNTriplesString(noteValue))){
                //the value is already present, so just return
                return;
            }
        }
        notePropToRemovedNoteValueListMap.get(propString).add(NTriplesUtil.toNTriplesString(noteValue));
    }

    public Map<String, List<String>> getNotePropToAddedNoteValueListMap() {
        return notePropToAddedNoteValueListMap;
    }

    public void addNotePropAndAddedNoteValue(IRI noteProp, Literal noteValue){
        if(noteProp == null || noteValue == null){
            return;
        }
        String propString = NTriplesUtil.toNTriplesString(noteProp);
        if(!notePropToAddedNoteValueListMap.containsKey(propString)){
            notePropToAddedNoteValueListMap.put(propString, new ArrayList<>());
        }
        //iterate to check if the value is already present
        for(String existingValue : notePropToAddedNoteValueListMap.get(propString)){
            if(existingValue.equals(NTriplesUtil.toNTriplesString(noteValue))){
                //the value is already present, so just return
                return;
            }
        }
        notePropToAddedNoteValueListMap.get(propString).add(NTriplesUtil.toNTriplesString(noteValue));
    }
}
