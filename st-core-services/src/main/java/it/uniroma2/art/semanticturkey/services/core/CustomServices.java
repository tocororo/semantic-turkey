package it.uniroma2.art.semanticturkey.services.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import it.uniroma2.art.semanticturkey.services.Response;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

@STService
public class CustomServices extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(CustomServices.class);

	@Autowired
	private ConfigurableListableBeanFactory context;

	@STServiceOperation
	public void registerCustomService() throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Class<? extends Object> serviceClass = new ByteBuddy().subclass(Object.class)
				.annotateType(AnnotationDescription.Builder.ofType(STService.class).build())
				.defineField("stServiceContext", STServiceContext.class, Modifier.PUBLIC)
				.annotateField(AnnotationDescription.Builder.ofType(Autowired.class).build())
				.defineMethod("sayHello", String.class, Modifier.PUBLIC)
				.intercept(InvocationHandlerAdapter.of(new InvocationHandler() {

					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						STServiceContext stServiceContext = (STServiceContext) proxy.getClass()
								.getDeclaredField("stServiceContext").get(proxy);
						System.out.println(stServiceContext.getContextParameter("project"));
						return "hello, world";
					}
				})).make().load(getClass().getClassLoader()).getLoaded();
		Object service = context.createBean(serviceClass, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
		Object controller = new ByteBuddy().subclass(Object.class)
				.annotateType(AnnotationDescription.Builder.ofType(Controller.class).build())
				.defineMethod("sayHello",
						TypeDescription.Generic.Builder.parameterizedType(Response.class, String.class)
								.build(),
						Modifier.PUBLIC)
				.intercept(InvocationHandlerAdapter.of(new InvocationHandler() {

					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						Method m = service.getClass().getMethod("sayHello");
						Object out = m.invoke(service);
						System.out.println("@@@@ output is = " + out);
						return new Response<>((String) out);
					}
				}))
				.annotateMethod(AnnotationDescription.Builder.ofType(RequestMapping.class)
						.defineArray("value",
								"it.uniroma2.art.semanticturkey/st-custom-services/Test/sayHello")
						.defineEnumerationArray("method", RequestMethod.class, RequestMethod.GET)
						.defineArray("produces", "application/json").build())
				.annotateMethod(AnnotationDescription.Builder.ofType(ResponseBody.class).build()).make()
				.load(getClass().getClassLoader()).getLoaded().newInstance();

		context.registerSingleton("test-service", service);
		context.registerSingleton("test-controller", controller);
	}
}
