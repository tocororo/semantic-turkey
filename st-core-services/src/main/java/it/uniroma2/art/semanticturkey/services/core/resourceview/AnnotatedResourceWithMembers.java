package it.uniroma2.art.semanticturkey.services.core.resourceview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import it.uniroma2.art.semanticturkey.services.AnnotatedValue;

@JsonSerialize(using = AnnotatedResourceWithMembersSerializer.class)
public class AnnotatedResourceWithMembers<T extends Resource, Q extends Value> extends AnnotatedValue<T> {
	private List<AnnotatedValue<Q>> members;

	public AnnotatedResourceWithMembers(T value) {
		this(value, new ArrayList<>());
	}

	public AnnotatedResourceWithMembers(T value, List<AnnotatedValue<Q>> members) {
		this(value, new HashMap<>(), members);
	}

	public AnnotatedResourceWithMembers(T value, Map<String, Value> attributes) {
		super(value, attributes);
	}

	public AnnotatedResourceWithMembers(T value, Map<String, Value> attributes,
			List<AnnotatedValue<Q>> members) {
		super(value, attributes);
		this.members = members;
	}

	public List<AnnotatedValue<Q>> getMembers() {
		return members;
	}
}
