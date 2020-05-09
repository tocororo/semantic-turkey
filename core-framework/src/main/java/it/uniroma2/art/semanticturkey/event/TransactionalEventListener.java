package it.uniroma2.art.semanticturkey.event;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * An {@link EventListener} bound to a phase of the current transaction. This listener should be used when an
 * event is posted during a transaction, and the corresponding handler should be executed <em>before the
 * commit</em>, <em>after the commit</em> or <em>after the rollback</em>.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public interface TransactionalEventListener<T extends Event> extends EventListener<T> {

	@Override
	default void onApplicationEvent(T event) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

				@Override
				public void suspend() {
				}

				@Override
				public void resume() {
				}

				@Override
				public void flush() {
				}

				@Override
				public void beforeCompletion() {
				}

				@Override
				public void beforeCommit(boolean readOnly) {
					TransactionalEventListener.this.beforeCommit(event);
				}

				@Override
				public void afterCompletion(int status) {
					if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
						TransactionalEventListener.this.afterRollback(event);
					}
				}

				@Override
				public void afterCommit() {
					TransactionalEventListener.this.afterCommit(event);
				}
			});
		}
	}

	/**
	 * Handle an event before the contextual transaction is committed.
	 * 
	 * @param event
	 */
	default void beforeCommit(T event) {
	}

	/**
	 * Handle the event after the contextual transaction is committed.
	 * 
	 * @param event
	 */
	default void afterCommit(T event) {
	}

	/**
	 * Handle the event after the contextual transaction is rolled back.
	 * 
	 * @param event
	 */
	default void afterRollback(T event) {
	}
}
