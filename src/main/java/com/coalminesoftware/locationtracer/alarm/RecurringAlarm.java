package com.coalminesoftware.locationtracer.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;

public abstract class RecurringAlarm extends BroadcastReceiver {
	private static final String BASE_ALARM_ACTION = "com.coalminesoftware.locationtracer.TRIGGER_RECURRING_ALARM";
	private static int INSTANCE_COUNT = 0;

	private Context context;
	private long alarmIntervalDuration;
	private boolean wakeForAlarm;

	private String alarmIntentAction;
	private PendingIntent alarmPendingIntent;

	public RecurringAlarm(Context context, long alarmIntervalDuration, boolean wakeForAlarm) {
		this.context = context.getApplicationContext();
		this.alarmIntervalDuration = alarmIntervalDuration;
		this.wakeForAlarm = wakeForAlarm;

		alarmIntentAction = generateIntentAction(context);
		alarmPendingIntent = buildPendingIntent(context, alarmIntentAction, alarmIntervalDuration, wakeForAlarm);
	}

	public abstract void handleAlarm();

	public void startRecurringReporting() {
		register();
		scheduleAlaram();
	}

	public void stopRecurringReporting() {
		cancelAlarm();
		unregister();
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		handleAlarm();
		scheduleAlaram();
	}

	private void register() {
		context.registerReceiver(this, new IntentFilter(alarmIntentAction));
	}

	private void unregister() {
		context.unregisterReceiver(this);
	}

	private void scheduleAlaram() {
		int alarmType = wakeForAlarm? AlarmManager.ELAPSED_REALTIME_WAKEUP : AlarmManager.ELAPSED_REALTIME;

		getAlarmManager(context).set(alarmType,
				SystemClock.elapsedRealtime() + alarmIntervalDuration,
				alarmPendingIntent);
	}

	private void cancelAlarm() {
		getAlarmManager(context).cancel(alarmPendingIntent);
	}

	private static AlarmManager getAlarmManager(Context context) {
		return (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
	}

	private static String generateIntentAction(Context context) {
		return context.getPackageName() + "/" +
				BASE_ALARM_ACTION + "/" +
				INSTANCE_COUNT++;
	}

	private static PendingIntent buildPendingIntent(Context context, String reportLocationsIntentAction,
			long reportIntervalDuration, boolean wakeForReport) {
		Intent intent = new Intent(reportLocationsIntentAction);
		return PendingIntent.getBroadcast(context, 0, intent, 0);
	}
}
