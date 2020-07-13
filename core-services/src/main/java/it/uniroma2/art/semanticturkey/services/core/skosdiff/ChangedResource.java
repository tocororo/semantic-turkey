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

public class ChangedResource {
    private String resourceId; //NTriplesUtil.toNTriplesString(resource)

    //the list of String are Value transformed in NT, via the function: NTriplesUtil.toNTriplesString(Value)
    private Map<String, List<String>> lexPropToRemovedLexicalizationListMap = new HashMap<>(); //the lexProp is in <lexProp>, lexicalization present in dataset1 but not in dataset2
    private Map<String, List<String>> lexPropToAddedLexicalizationListMap= new HashMap<>(); //the lexProp is in <lexProp>, lexicalization present in dataset2 but not in dataset1

    private Map<String, List<String>> notePropToRemovedNoteValueListMap = new HashMap<>(); //the noteProp is in <noteProp>, noteValue present in dataset1 but not in dataset2
    private Map<String, List<String>> notePropToAddedNoteValueListMap= new HashMap<>(); //the noteProp is in <noteProp>, noteValue present in dataset2 but not in dataset1

    private Map<String, List<String>> propToRemovedValueListMap = new HashMap<>(); //the prop is in <propIRI>, prop-value present in dataset1 but not in dataset2
    private Map<String, List<String>> propToAddedValueListMap = new HashMap<>(); //the prop is in <propIRI>, prop-value present in dataset2 but not in dataset1


    public ChangedResource() {
    }

    public ChangedResource(IRI resourceId) {
        this.resourceId = NTriplesUtil.toNTriplesString(resourceId);
    }

    public String getResourceId() {
        return resourceId;
    }

    public Map<String, List<String>> getLexPropToRemovedLexicalizationListMap() {
        return lexPropToRemovedLexicalizationListMap;
    }

    public void addLexPropAndRemovedLexicalization(IRI lexProp, Literal lexicalization){
        if(lexProp==null || lexicalization == null){
            return;
        }
        String lexPropString = NTriplesUtil.toNTriplesString(lexProp);
        if(!lexPropToRemovedLexicalizationListMap.containsKey(lexPropString)){
            lexPropToRemovedLexicalizationListMap.put(lexPropString, new ArrayList<>());
        }
        //iterate to check if the lexicalization is already present
        for(String existingLex : lexPropToRemovedLexicalizationListMap.get(lexPropString)){
            if(existingLex.equals(NTriplesUtil.toNTriplesString(lexicalization))){
                //the lexicalization is already present, so just return
                return;
            }
        }
        lexPropToRemovedLexicalizationListMap.get(lexPropString).add(NTriplesUtil.toNTriplesString(lexicalization));
    }


    public Map<String, List<String>> getLexPropToAddedLexicalizationListMap() {
        return lexPropToAddedLexicalizationListMap;
    }

    public void addLexPropAndAddedLexicalization(IRI lexProp, Literal lexicalization){
        if(lexProp==null || lexicalization == null){
            return;
        }
        String lexPropString = NTriplesUtil.toNTriplesString(lexProp);
        if(!lexPropToAddedLexicalizationListMap.containsKey(lexPropString)){
            lexPropToAddedLexicalizationListMap.put(lexPropString, new ArrayList<>());
        }
        //iterate to check if the lexicalization is already present
        for(String existingLex : lexPropToAddedLexicalizationListMap.get(lexPropString)){
            if(existingLex.equals(NTriplesUtil.toNTriplesString(lexicalization))){
                //the lexicalization is already present, so just return
                return;
            }
        }
        lexPropToAddedLexicalizationListMap.get(lexPropString).add(NTriplesUtil.toNTriplesString(lexicalization));
    }


    public Map<String, List<String>> getNotePropToRemovedNoteValueListMap() {
        return notePropToRemovedNoteValueListMap;
    }

    public void addNotePropAndRemovedNoteValue(IRI noteProp, Literal noteValue){
        if(noteProp==null || noteValue == null){
            return;
        }
        String notePropString = NTriplesUtil.toNTriplesString(noteProp);
        if(!notePropToRemovedNoteValueListMap.containsKey(notePropString)){
            notePropToRemovedNoteValueListMap.put(notePropString, new ArrayList<>());
        }
        //iterate to check if the noteValue is already present
        for(String existingNoteValue : notePropToRemovedNoteValueListMap.get(notePropString)){
            if(existingNoteValue.equals(NTriplesUtil.toNTriplesString(noteValue))){
                //the noteValue is already present, so just return
                return;
            }
        }
        notePropToRemovedNoteValueListMap.get(notePropString).add(NTriplesUtil.toNTriplesString(noteValue));
    }

    public Map<String, List<String>> getNotePropToAddedNoteValueListMap() {
        return notePropToAddedNoteValueListMap;
    }

    public void addNotePropAndAddedNoteValue(IRI noteProp, Literal noteValue){
        if(noteProp==null || noteValue == null){
            return;
        }
        String notePropString = NTriplesUtil.toNTriplesString(noteProp);
        if(!notePropToAddedNoteValueListMap.containsKey(notePropString)){
            notePropToAddedNoteValueListMap.put(notePropString, new ArrayList<>());
        }
        //iterate to check if the lexicalization is already present
        for(String existingNoteValue : notePropToAddedNoteValueListMap.get(notePropString)){
            if(existingNoteValue.equals(NTriplesUtil.toNTriplesString(noteValue))){
                //the noteVaòie is already present, so just return
                return;
            }
        }
        notePropToAddedNoteValueListMap.get(notePropString).add(NTriplesUtil.toNTriplesString(noteValue));
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


}
