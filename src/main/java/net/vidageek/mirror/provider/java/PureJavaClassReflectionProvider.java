package net.vidageek.mirror.provider.java;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.vidageek.mirror.exception.ReflectionProviderException;
import net.vidageek.mirror.matcher.ClassArrayMatcher;
import net.vidageek.mirror.matcher.MatchType;
import net.vidageek.mirror.provider.ClassReflectionProvider;

public final class PureJavaClassReflectionProvider<T> implements ClassReflectionProvider<T> {
	private static final Map<Class<?>, List<Method>> allMethodsCache = new ConcurrentHashMap<Class<?>, List<Method>>();
	private static final Map<Class<?>, List<Field>> allFieldsCache = new ConcurrentHashMap<Class<?>, List<Field>>();
	private static final Map<Class<?>, List<Constructor<?>>> allConstructorsCache = new ConcurrentHashMap<Class<?>, List<Constructor<?>>>();

	private Class<T> clazz;

	@SuppressWarnings("unchecked")
	public PureJavaClassReflectionProvider(final String className) {
		try {
			this.clazz = (Class<T>) Class.forName(className, false, PureJavaClassReflectionProvider.class
					.getClassLoader());
		} catch (final ClassNotFoundException e) {
			this.clazz = (Class<T>) FixedType.fromValue(className);

			if (clazz == null) {
				throw new ReflectionProviderException("class " + className + " could not be found.", e);
			}
		}
	}

	public PureJavaClassReflectionProvider(final Class<T> clazz) {
		this.clazz = clazz;
	}

	public Class<T> reflectClass() {
		return clazz;
	}

	public List<Field> reflectAllFields() {
		List<Field> list = (List) PureJavaClassReflectionProvider.allFieldsCache.get(clazz);
		if (list == null) {
			list = new ArrayList<Field>();
			for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
				for (Field field : current.getDeclaredFields()) {
					list.add(field);
				}
				for (Class<?> interf : current.getInterfaces()) {
					for (Field field : interf.getDeclaredFields()) {
						list.add(field);
					}
				}
			}

			PureJavaClassReflectionProvider.allFieldsCache.put(clazz, list);
		}

		return list;
	}

	public List<Method> reflectAllMethods() {
		List<Method> list = (List) PureJavaClassReflectionProvider.allMethodsCache.get(clazz);

		if (list == null) {
			list = new ArrayList<Method>();
			for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
				for (Method method : current.getDeclaredMethods()) {
					list.add(method);
				}
			}
			PureJavaClassReflectionProvider.allMethodsCache.put(clazz, list);
		}

		return list;
	}

	// If somebody can tell me why on earth getDeclaredConstructors return
	// Constructor<?>[], I would be really glad.
	@SuppressWarnings("unchecked")
	public List<Constructor<T>> reflectAllConstructors() {
		List<Constructor<T>> list = (List) PureJavaClassReflectionProvider.allConstructorsCache.get(clazz);
		if (list == null) {
			list = (List) Arrays.asList(clazz.getDeclaredConstructors());
			PureJavaClassReflectionProvider.allConstructorsCache.put(clazz, (List) list);
		}
		return list;
	}

	public Constructor<T> reflectConstructor(final Class<?>[] argumentTypes) {
		final ClassArrayMatcher matcher = new ClassArrayMatcher(argumentTypes);

		Constructor<T> match = null;
		for (final Constructor<T> constructor : reflectAllConstructors()) {
			final MatchType matchType = matcher.match(constructor.getParameterTypes());
			if (MatchType.PERFECT.equals(matchType)) {
				match = constructor;
				break;
			} else if (MatchType.MATCH.equals(matchType)) {
				match = constructor;
			}
		}
		return match;
	}

	public Field reflectField(final String fieldName) {
		for (final Field f : reflectAllFields()) {
			if (f.getName().equals(fieldName)) {
				return f;
			}
		}
		return null;
	}

	public Method reflectMethod(final String methodName, final Class<?>[] argumentTypes) {
		final ClassArrayMatcher matcher = new ClassArrayMatcher(argumentTypes);

		Method match = null;
		for (final Method method : reflectAllMethods()) {
			if (method.getName().equals(methodName)) {
				final MatchType matchType = matcher.match(method.getParameterTypes());
				if (MatchType.PERFECT.equals(matchType)) {
					match = method;
					break;
				} else if (MatchType.MATCH.equals(matchType)) {
					match = method;
				}
			}
		}
		return match;
	}

}
