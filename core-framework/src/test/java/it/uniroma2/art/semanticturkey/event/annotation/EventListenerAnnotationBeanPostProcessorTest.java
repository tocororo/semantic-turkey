package it.uniroma2.art.semanticturkey.event.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Optional;

import org.aopalliance.aop.Advice;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.assertThat;

import it.uniroma2.art.semanticturkey.event.Event;
import it.uniroma2.art.semanticturkey.event.annotation.TransactionalEventListener.Phase;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryTransactionManager;

public class EventListenerAnnotationBeanPostProcessorTest {

	public static class TestEvent extends Event {

		private static final long serialVersionUID = 1L;

		public TestEvent(Object source) {
			super(source);
		}

	}

	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Adviced {

	}

	public static class TestAdvisor implements PointcutAdvisor {

		private boolean invoked;

		@Override
		public Advice getAdvice() {
			return new MethodBeforeAdvice() {

				@Override
				public void before(Method method, Object[] args, Object target) throws Throwable {
					invoked = true;
				}
			};
		}

		@Override
		public boolean isPerInstance() {
			return true;
		}

		@Override
		public Pointcut getPointcut() {
			return AnnotationMatchingPointcut.forMethodAnnotation(Adviced.class);
		}

		public boolean isInvoked() {
			return invoked;
		}

	}

	public static class AnnotatedClass {

		public boolean invokedOrdinary;
		public boolean invokedTransactional;

		public boolean isInvokedOrdinary() {
			return invokedOrdinary;
		}

		public boolean isInvokedTransactional() {
			return invokedTransactional;
		}

		@Adviced
		@EventListener
		public void handleEvent(TestEvent event) {
			invokedOrdinary = true;
		}

		@Adviced
		@TransactionalEventListener(phase = Phase.beforeCommit)
		public void handleEventTransactionally(TestEvent event) {
			invokedTransactional = true;
		}
	}

	private StaticApplicationContext applicationContext;

	@Before
	public void tearUp() {
		applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton(EventListenerAnnotationBeanPostProcessor.class.getName(),
				EventListenerAnnotationBeanPostProcessor.class);
		applicationContext.registerSingleton(AnnotatedClass.class.getName(), AnnotatedClass.class);
		applicationContext.registerSingleton(DefaultAdvisorAutoProxyCreator.class.getName(),
				DefaultAdvisorAutoProxyCreator.class);
		applicationContext.registerSingleton(TestAdvisor.class.getName(), TestAdvisor.class);
		applicationContext.refresh();
	}

	@After
	public void tearDown() {
		if (applicationContext != null) {
			applicationContext.close();
		}
	}

	@Test
	public void testOrdinary() {
		applicationContext.publishEvent(new TestEvent(this));

		AnnotatedClass annotatedClass = applicationContext.getBean(AnnotatedClass.class);

		assertThat(annotatedClass.isInvokedOrdinary(), Matchers.is(true));
		assertThat(annotatedClass.isInvokedTransactional(), Matchers.is(false));
	}

	@Test
	public void testTransactional() {
		Repository rep = new SailRepository(new MemoryStore());

		new TransactionTemplate(new RDF4JRepositoryTransactionManager(rep, Optional.empty()))
				.execute(status -> {
					applicationContext.publishEvent(new TestEvent(this));
					return null;
				});
		AnnotatedClass annotatedClass = applicationContext.getBean(AnnotatedClass.class);

		assertThat(annotatedClass.isInvokedOrdinary(), Matchers.is(true));
		assertThat(annotatedClass.isInvokedTransactional(), Matchers.is(true));
	}

}
