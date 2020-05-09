package it.uniroma2.art.semanticturkey.event.annotation;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import it.uniroma2.art.semanticturkey.event.Event;
import it.uniroma2.art.semanticturkey.event.annotation.TransactionalEventListener.Phase;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;

/**
 * A {@link BeanPostProcessor} that registers {@link EventListener}s bound to bean methods annotated with
 * {@link EventListener} or {@link TransactionalEventListener}.
 * 
 * @author <a href="mailto:fiorelli@uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class EventListenerAnnotationBeanPostProcessor
		implements BeanPostProcessor, ApplicationContextAware, DestructionAwareBeanPostProcessor {

	public static class MethodInvocationEventListener<T extends Event>
			implements it.uniroma2.art.semanticturkey.event.EventListener<T> {

		private ApplicationContext applicationContext;
		private String beanName;
		private Method method;

		private Object obj;

		public MethodInvocationEventListener(ApplicationContext applicationContext, String beanName,
				Method method) {
			this.applicationContext = applicationContext;
			this.beanName = beanName;
			this.method = method;
		}

		@Override
		public void onApplicationEvent(T event) {
			if (obj == null) {
				obj = applicationContext.getBean(beanName);
			}

			try {
				method.invoke(obj, event);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}

	}

	public static class MethodInvocationTransactionalEventListener<T extends Event>
			implements it.uniroma2.art.semanticturkey.event.TransactionalEventListener<T> {

		private ApplicationContext applicationContext;
		private String beanName;
		private Method method;

		private Object obj;
		private Phase phase;

		public MethodInvocationTransactionalEventListener(ApplicationContext applicationContext,
				String beanName, Method method, TransactionalEventListener.Phase phase) {
			this.applicationContext = applicationContext;
			this.beanName = beanName;
			this.method = method;
			this.phase = phase;
		}

		@Override
		public void beforeCommit(T event) {
			if (phase == Phase.beforeCommit) {
				invoke(event);
			}
		};

		@Override
		public void afterCommit(T event) {
			if (phase == Phase.afterCommit) {
				invoke(event);
			}
		};

		@Override
		public void afterRollback(T event) {
			if (phase == Phase.afterRollback) {
				invoke(event);
			}
		}

		private void invoke(T event) {
			if (obj == null) {
				obj = applicationContext.getBean(beanName);
			}

			try {
				method.invoke(obj, event);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}

	}

	private ApplicationContext applicationContext;

	private Map<Type, WeakReference<Class<?>>> eventListenerClazzes = new HashMap<>();
	private Map<Type, WeakReference<Class<?>>> transactionalEventListenerClazzes = new HashMap<>();

	private Multimap<String, ApplicationListener<?>> bean2listeners = HashMultimap.create();

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		processBeanInternal(bean, beanName, EventListener.class, eventListenerClazzes,
				MethodInvocationEventListener.class);
		processBeanInternal(bean, beanName, TransactionalEventListener.class,
				transactionalEventListenerClazzes, MethodInvocationTransactionalEventListener.class);

		return bean;

	}

	protected void processBeanInternal(Object bean, String beanName,
			Class<? extends Annotation> annotationType,
			Map<Type, WeakReference<Class<?>>> eventListenerClazzes2, Class<?> eventHandlerGenericClass) {
		Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);

		List<Method> candidateEventListenerMethods = MethodUtils.getMethodsListWithAnnotation(targetClass,
				annotationType);

		candidateEventListenerMethods = filterCandidates(candidateEventListenerMethods);

		for (Method m : candidateEventListenerMethods) {
			Type eventType = m.getGenericParameterTypes()[0];

			Class<?> clazz = null;
			if (eventListenerClazzes2.containsKey(eventType)) {
				clazz = eventListenerClazzes2.get(eventType).get();
			}

			if (clazz == null) {
				clazz = new ByteBuddy()
						.subclass(TypeDescription.Generic.Builder
								.parameterizedType(eventHandlerGenericClass, eventType).build())
						.make().load(EventListenerAnnotationBeanPostProcessor.class.getClassLoader())
						.getLoaded();

				eventListenerClazzes2.put(eventType, new WeakReference<>(clazz));
			}

			try {
				ApplicationListener<?> listener = (ApplicationListener<?>) ConstructorUtils.invokeConstructor(
						clazz, computeMethodInvocationConstructorArgumnets(applicationContext, beanName, m,
								annotationType));
				((ConfigurableApplicationContext) applicationContext).addApplicationListener(listener);
			} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException
					| InstantiationException e) {
				e.printStackTrace();
			}

		}
	}

	private Object[] computeMethodInvocationConstructorArgumnets(ApplicationContext applicationContext,
			String beanName, Method method, Class<?> annotationType) {
		if (annotationType == TransactionalEventListener.class) {
			return new Object[] { applicationContext, beanName, method,
					method.getAnnotation(TransactionalEventListener.class).phase() };
		} else {
			return new Object[] { applicationContext, beanName, method };
		}
	}

	private List<Method> filterCandidates(List<Method> canidateEventListenerMethods) {
		return canidateEventListenerMethods.stream().filter(m -> {
			Type[] params = m.getGenericParameterTypes();
			return params.length == 1 && TypeUtils.isAssignable(params[0], Event.class);
		}).collect(Collectors.toList());
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		// see: https://stackoverflow.com/a/38515421
		ApplicationEventMulticaster aem = applicationContext.getBean(
				AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
				ApplicationEventMulticaster.class);
		;
		for (ApplicationListener<?> listener : bean2listeners.removeAll(beanName)) {
			aem.removeApplicationListener(listener);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
