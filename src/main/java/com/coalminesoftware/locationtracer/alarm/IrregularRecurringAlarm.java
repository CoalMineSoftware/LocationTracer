package com.coalminesoftware.locationtracer.alarm;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/**
 * An alarm that repeats at an irregular interval, determined by {@link #determineNextAlarmDelay(long)}.
 */
public abstract class IrregularRecurringAlarm extends BaseRecurringAlarm {
	public IrregularRecurringAlarm(Context context, boolean wakeForAlarm) {
		super(context, wakeForAlarm);
	}

	protected abstract long determineNextAlarmDelay(long alarmElapsedRealtime);

	@Override
	public void onReceive(Context context, Intent intent) {
		long elapsedRealtime = SystemClock.elapsedRealtime();

		handleAlarm(elapsedRealtime);
		scheduleAlarm(elapsedRealtime);
	}

	@Override
	protected void scheduleAlarm(long alarmElapsedRealtime) {
		getAlarmManager(getContext()).set(
				getAlarmType(),
				SystemClock.elapsedRealtime() + determineNextAlarmDelay(alarmElapsedRealtime),
				getAlarmPendingIntent());
	}
}
