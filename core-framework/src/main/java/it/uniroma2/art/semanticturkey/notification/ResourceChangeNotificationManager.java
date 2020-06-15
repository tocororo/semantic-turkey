package it.uniroma2.art.semanticturkey.notification;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.support.CronTrigger;

import it.uniroma2.art.semanticturkey.event.annotation.TransactionalEventListener;
import it.uniroma2.art.semanticturkey.event.annotation.TransactionalEventListener.Phase;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.services.events.ResourceCreated;
import it.uniroma2.art.semanticturkey.services.events.ResourceDeleted;
import it.uniroma2.art.semanticturkey.services.events.ResourceModified;
import it.uniroma2.art.semanticturkey.settings.notification.NotificationSystemSettings;
import it.uniroma2.art.semanticturkey.settings.notification.NotificationSystemSettings.CronDefinition;
import it.uniroma2.art.semanticturkey.settings.notification.NotificationSystemSettingsManager;

public class ResourceChangeNotificationManager {

	@Autowired
	private NotificationSystemSettingsManager systemSettingsManager;

	@Autowired
	private TaskScheduler taskScheduler;
	private ScheduledFuture<?> currentFuture = null;

	private AtomicBoolean notificationDigestSending = new AtomicBoolean(false);

	private class NotificationDigestRunnable implements Runnable {

		@Override
		public void run() {
			boolean updated = notificationDigestSending.compareAndSet(false, true);
			if (!updated)
				return; // another task is running, skip this one

			try {
				ResourceChangeNotificationManager.this.scheduledNotifications();
			} finally {
				notificationDigestSending.set(false);
			}
		}

	}

	@PostConstruct
	public void init() throws STPropertyAccessException {
		scheduleNotificationDigest();
	}

	@TransactionalEventListener(phase = Phase.afterCommit)
	@Async
	public void onCreation(ResourceCreated event) {
		System.out.println("Send notifications about creation of " + event.getResource());
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Finish notifications about creation of " + event.getResource());
	}

	@TransactionalEventListener(phase = Phase.afterCommit)
	@Async
	public void onUpdate(ResourceModified event) {
		System.out.println("Send notifications about modification of " + event.getResource());
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Finish notifications about modification of " + event.getResource());
	}

	@TransactionalEventListener(phase = Phase.afterCommit)
	@Async
	public void onDeletion(ResourceDeleted event) {
		System.out.println("Send notifications about deletion of " + event.getResource());
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Finish notifications about deletion of " + event.getResource());
	}

	public void scheduledNotifications() {
		System.out.println("Scheduled notifications");
	}

	public synchronized void setNotificationDigestSchedule(NotificationSystemSettings.CronDefinition schedule)
			throws STPropertyUpdateException {
		NotificationSystemSettings settings = new NotificationSystemSettings();
		settings.notificationDigestSchedule = new NotificationSystemSettings.CronDefinition();
		settings.notificationDigestSchedule.expression = schedule.expression;
		settings.notificationDigestSchedule.zone = schedule.zone;

		systemSettingsManager.storeSystemSettings(settings);
		
		scheduleNotificationDigest(settings);
	}

	public synchronized void disableNotificationDigest() throws STPropertyUpdateException {
		NotificationSystemSettings settings = new NotificationSystemSettings();
		settings.notificationDigestSchedule = null;

		systemSettingsManager.storeSystemSettings(settings);
		
		scheduleNotificationDigest(settings);
	}

	protected synchronized void scheduleNotificationDigest() throws STPropertyAccessException {
		NotificationSystemSettings settings = systemSettingsManager.getSystemSettings();
		scheduleNotificationDigest(settings);
	}

	protected synchronized void scheduleNotificationDigest(NotificationSystemSettings settings) {
		if (currentFuture != null) {
			currentFuture.cancel(true);
			currentFuture = null;
		}

		CronDefinition cron = settings.notificationDigestSchedule;

		if (cron != null) {
			currentFuture = taskScheduler.schedule(new NotificationDigestRunnable(), buildTrigger(cron));
		}
	}

	protected CronTrigger buildTrigger(CronDefinition cron) {
		if (cron.zone != null) {
			return new CronTrigger(cron.expression, cron.zone);
		} else {
			return new CronTrigger(cron.expression);
		}
	}
}
