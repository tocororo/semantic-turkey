package it.uniroma2.art.semanticturkey.event;

import java.io.IOException;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.StaticApplicationContext;

import static org.junit.Assert.assertThat;

public class EventListenerTest {

	protected static class EventA extends Event {

		private static final long serialVersionUID = 7146091162387184320L;

		public EventA(Object source) {
			super(source);
		}
	}

	protected static class EventASub extends EventA {

		private static final long serialVersionUID = 7146091162387184322L;

		public EventASub(Object source) {
			super(source);
		}
	}

	protected static class EventB extends Event {

		private static final long serialVersionUID = 7146091162387184321L;

		public EventB(Object source) {
			super(source);
		}
	}

	protected StaticApplicationContext applicationContext;
	protected MutableBoolean delivered;

	@Before
	public void tearUp() {
		applicationContext = new StaticApplicationContext();
		delivered = new MutableBoolean(false);
		applicationContext.addApplicationListener(new EventListener<EventA>() {

			@Override
			public void onApplicationEvent(EventA event) {
				delivered.setTrue();
			}

		});
		applicationContext.refresh();
	}

	@After
	public void tearDown() throws IOException {
		if (applicationContext != null) {
			applicationContext.close();
		}
	}

	@Test
	public void testExactEventType() {

		applicationContext.publishEvent(new EventA(this));

		assertThat(delivered.booleanValue(), Matchers.is(true));
	}

	@Test
	public void testEventSubtype() {

		applicationContext.publishEvent(new EventASub(this));

		assertThat(delivered.booleanValue(), Matchers.is(true));
	}

	@Test
	public void testUnrelatedEventType() {

		applicationContext.publishEvent(new EventB(this));

		assertThat(delivered.booleanValue(), Matchers.is(false));
	}

	@Test
	public void testUnrelatedEventSypeType() {

		applicationContext.publishEvent(new Event(this));

		assertThat(delivered.booleanValue(), Matchers.is(false));
	}

}
