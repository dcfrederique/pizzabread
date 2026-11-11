package com.dcfrederique.pizzabread.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dcfrederique.pizzabread.MainActivity
import com.dcfrederique.pizzabread.domain.Bake
import com.dcfrederique.pizzabread.domain.MINUTE_MS
import com.dcfrederique.pizzabread.domain.Planner

/**
 * Bij Android staat altijd precies één alarm: het eerstvolgende moment uit de planning.
 * Gaat het af, dan toont [AlarmReceiver] de melding en plant meteen het volgende. Omdat de
 * planning als absolute tijden bewaard is, kan alles na een herstart opnieuw opgebouwd worden.
 */
object AlarmScheduler {
    const val EXTRA_AT = "at"
    const val EXTRA_KIND = "kind"
    const val KIND_STEP = "step"
    const val KIND_PAUSE = "pause"
    const val PAUSE_REMINDER_MINUTES = 30

    fun canScheduleExact(context: Context): Boolean {
        val am = context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    fun schedule(context: Context, bake: Bake?, after: Long = System.currentTimeMillis()) {
        val am = context.getSystemService(AlarmManager::class.java)
        cancel(context)
        if (bake == null) return
        val now = System.currentTimeMillis()

        val (at, kind) = if (bake.pausedAt != null) {
            val remindAt = bake.pausedAt + PAUSE_REMINDER_MINUTES * MINUTE_MS
            if (remindAt <= now) return
            remindAt to KIND_PAUSE
        } else {
            val next = Planner.nextGroup(bake, now, after).firstOrNull() ?: return
            next.at to KIND_STEP
        }

        val operation = receiverIntent(context, at, kind)
        if (canScheduleExact(context)) {
            val show = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            // setAlarmClock gaat exact af, ook in Doze, net als de klok-app.
            am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), operation)
        } else {
            // Terugval zonder recht op exacte alarmen: kan enkele minuten te laat komen.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(receiverIntent(context, 0, KIND_STEP))
    }

    private fun receiverIntent(context: Context, at: Long, kind: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(EXTRA_AT, at)
            .putExtra(EXTRA_KIND, kind)
        return PendingIntent.getBroadcast(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
