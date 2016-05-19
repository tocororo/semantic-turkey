package it.uniroma2.art.semanticturkey.utilities;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
}
