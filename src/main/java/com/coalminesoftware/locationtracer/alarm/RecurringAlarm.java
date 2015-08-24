package com.coalminesoftware.locationtracer.alarm;

import android.content.Context;
import android.os.SystemClock;

/**
 * An alarm that schedules itself to repeat at a regular interval.
 */
public abstract class RecurringAlarm extends BaseRecurringAlarm {
	private long alarmIntervalDuration;

	public RecurringAlarm(Context context, long alarmIntervalDuration, boolean wakeForAlarm) {
		super(context, wakeForAlarm);

		this.alarmIntervalDuration = alarmIntervalDuration;
	}

	@Override
	protected void scheduleAlarm(long alarmElapsedRealtime) {
		// All repeating alarms are inexact on Android 4.4+ so this code requests inexact alarms specifically, to ensure
		// similar behavior between pre- and post-KitKat devices.
		getAlarmManager(getContext()).setInexactRepeating(
				getAlarmType(),
				SystemClock.elapsedRealtime() + alarmIntervalDuration,
				alarmIntervalDuration,
				getAlarmPendingIntent());
	}
}
