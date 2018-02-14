package it.uniroma2.art.semanticturkey.utilities;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.reflect.TypeUtils;

import com.google.common.collect.Iterables;

public abstract class ReflectionUtilities {
	/**
	 * Returns a method of a class unambiguously identified by its name. If there is no such method or there
	 * are many equally named methods, an {@link IllegalAccessException} is thrown.
	 * 
	 * @param clazz
	 * @param methodName
	 * @return
	 */
	public static Method getMethodByName(Class<?> clazz, String methodName) {
		List<Method> candidateMethods = Arrays.stream(clazz.getMethods())
				.filter(m -> m.getName().equals(methodName)).collect(Collectors.toList());
		return Iterables.getOnlyElement(candidateMethods);
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<T> getInterfaceArgumentTypeAsClass(Class<?> sourceClass, Class<?> targetClass,
			int argPos) {
		for (Type t : sourceClass.getGenericInterfaces()) {
			Map<TypeVariable<?>, Type> typeArgs = TypeUtils.getTypeArguments(t, targetClass);
			if (typeArgs != null) {
				for (Entry<TypeVariable<?>, Type> entry : typeArgs.entrySet()) {
					if (targetClass.getTypeParameters()[argPos].equals(entry.getKey())) {
						return (Class<T>) TypeUtils.getRawType(entry.getValue(), null);
					}
				}
			}
		}
		throw new IllegalStateException("Could not determine type argument");
	}
}
