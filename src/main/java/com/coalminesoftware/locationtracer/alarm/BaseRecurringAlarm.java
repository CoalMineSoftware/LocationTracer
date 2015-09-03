package com.coalminesoftware.locationtracer.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;

public abstract class BaseRecurringAlarm extends BroadcastReceiver {
	private static final String BASE_ALARM_ACTION = "com.coalminesoftware.locationtracer.TRIGGER_RECURRING_ALARM";
	private static int GENERATED_ACTION_COUNT = 0;

	private Context context;
	private int alarmType;

	private String alarmAction;
	private PendingIntent alarmIntent;

	public BaseRecurringAlarm(Context context, boolean wakeForAlarm) {
		this.context = context.getApplicationContext();
		this.alarmType = determineAlarmType(wakeForAlarm);

		alarmAction = generateAction(context);
		alarmIntent = buildPendingIntent(context, alarmAction);
	}

	public abstract void handleAlarm(long alarmElapsedRealtime);

	public void startRecurringAlarm() {
		registerAlarmReceiver();
		scheduleAlarm(SystemClock.elapsedRealtime());
	}

	public void stopRecurringAlarm() {
		cancelAlarm();
		unregisterAlarmReceiver();
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		handleAlarm(SystemClock.elapsedRealtime());
	}

	private void registerAlarmReceiver() {
		context.registerReceiver(this, new IntentFilter(alarmAction));
	}

	private void unregisterAlarmReceiver() {
		context.unregisterReceiver(this);
	}

	protected abstract void scheduleAlarm(long alarmElapsedRealtime);

	protected int determineAlarmType(boolean wakeForAlarm) {
		return wakeForAlarm? AlarmManager.ELAPSED_REALTIME_WAKEUP : AlarmManager.ELAPSED_REALTIME;
	}

	private void cancelAlarm() {
		getAlarmManager(context).cancel(alarmIntent);
	}

	protected static AlarmManager getAlarmManager(Context context) {
		return (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
	}

	protected Context getContext() {
		return context;
	}

	protected int getAlarmType() {
		return alarmType;
	}

	protected PendingIntent getAlarmPendingIntent() {
		return alarmIntent;
	}

	private static String generateAction(Context context) {
		return context.getPackageName() + "/" +
				BASE_ALARM_ACTION + "/" +
				GENERATED_ACTION_COUNT++;
	}

	private static PendingIntent buildPendingIntent(Context context, String action) {
		Intent intent = new Intent(action);
		return PendingIntent.getBroadcast(context, 0, intent, 0);
	}
}
