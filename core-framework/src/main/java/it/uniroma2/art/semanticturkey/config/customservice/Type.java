package it.uniroma2.art.semanticturkey.config.customservice;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import it.uniroma2.art.semanticturkey.properties.STPropertiesSerializer;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.eclipse.rdf4j.query.TupleQueryResult;

import javax.lang.model.type.ArrayType;

/**
 * Type information used in the definition of a custom service.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 * @see CustomService
 */
public class Type {

	private String name;
	private List<Type> typeArguments;

	@JsonCreator
	public Type(@JsonProperty(value = "name", required = true) String name,
			@JsonProperty(value = "typeArguments", required = false) List<Type> typeArguments) {
		this.name = name;
		this.typeArguments = typeArguments;
	}

	public String getName() {
		return name;
	}

	public List<Type> getTypeArguments() {
		return typeArguments;
	}

	public static Type fromJavaType(java.lang.reflect.Type javaType) {
		String typeName;
		if (TypeUtils.getRawType(javaType, null) == AnnotatedValue.class) {
			typeName = "AnnotatedValue";
		} else if (TypeUtils.equals(javaType, TupleQueryResult.class)) {
			typeName = "TupleQueryResult";
		} else {
			typeName = STPropertiesSerializer.computeReducedTypeName(javaType);
		}
		List<Type> typeArguments;
		if (javaType instanceof ParameterizedType) {
			typeArguments = Arrays.stream(((ParameterizedType) javaType).getActualTypeArguments())
					.map(Type::fromJavaType)
					.collect(Collectors.toList());
		} else if (TypeUtils.isArrayType(javaType)) {
			typeArguments = Lists.newArrayList(Type.fromJavaType(TypeUtils.getArrayComponentType(javaType)));
		} else {
			typeArguments = new ArrayList<>();
		}
		return new Type(typeName, typeArguments);
	}

	@Override
	public String toString() {
		if (typeArguments == null || typeArguments.isEmpty()) {
			return name;
		}
		return typeArguments.stream().map(Object::toString).collect(Collectors.joining(",", name + "<", ">"));
	}

}
