package com.coalminesoftware.locationtracer.alarm;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/**
 * An alarm that repeats at an irregular interval, determined by {@link #determineNextAlarmDelay()}.
 */
public abstract class IrregularRecurringAlarm extends BaseRecurringAlarm {
	public IrregularRecurringAlarm(Context context, boolean wakeForAlarm) {
		super(context, wakeForAlarm);
	}

	protected abstract long determineNextAlarmDelay();

	@Override
	protected void scheduleAlarm() {
		getAlarmManager(getContext()).set(
				getAlarmType(),
				SystemClock.elapsedRealtime() + determineNextAlarmDelay(),
				getAlarmPendingIntent());
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);

		scheduleAlarm();
	}
}
