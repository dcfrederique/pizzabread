package com.dcfrederique.pizzabread.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dcfrederique.pizzabread.MainActivity
import com.dcfrederique.pizzabread.R
import com.dcfrederique.pizzabread.domain.AlarmEvent
import com.dcfrederique.pizzabread.domain.Bake
import com.dcfrederique.pizzabread.domain.EventType
import com.dcfrederique.pizzabread.domain.MINUTE_MS
import com.dcfrederique.pizzabread.domain.Planner
import com.dcfrederique.pizzabread.domain.TimeFormat

object Notifications {
    const val CHANNEL_ALARM = "broodalarm"
    const val CHANNEL_INFO = "vooraankondiging"
    const val CHANNEL_STATUS = "lopende_bak"

    private const val ID_ONGOING = 1
    private const val ID_ALARM = 2
    private const val ID_INFO = 3

    /** Een alarm rinkelt maximaal zo lang, net als in de analyse. */
    private const val ALARM_TIMEOUT_MS = 2 * MINUTE_MS

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarm = NotificationChannel(CHANNEL_ALARM, "Broodalarm", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alarm bij elke stap van je bak"
            setSound(
                alarmSound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 600, 400, 600, 400, 600)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val info = NotificationChannel(CHANNEL_INFO, "Vooraankondigingen", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Korte melding vijf minuten vóór vormen of bakken"
        }
        val status = NotificationChannel(CHANNEL_STATUS, "Lopende bak", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Vaste melding met de volgende stap en de aftelling"
            setShowBadge(false)
        }
        nm.createNotificationChannels(listOf(alarm, info, status))
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun canUseFullScreen(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    @SuppressLint("MissingPermission")
    private fun post(context: Context, id: Int, notification: Notification) {
        if (canPost(context)) NotificationManagerCompat.from(context).notify(id, notification)
    }

    /** Volledig-scherm-alarm met geluid tot je reageert (of max. 2 minuten). */
    fun showAlarm(context: Context, events: List<AlarmEvent>) {
        val title = events.joinToString(" + ") { it.title }
        val text = events.firstOrNull { it.text.isNotBlank() }?.text.orEmpty()
        val step = events.firstOrNull { it.type == EventType.STEP || it.type == EventType.REMINDER }?.stepIndex ?: -1

        val fullScreen = PendingIntent.getActivity(
            context, 10,
            AlarmActivity.intent(context, title, text, step),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_stat_bread)
            .setContentTitle(title)
            .setContentText(text.ifBlank { "Tik om te openen" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .setDeleteIntent(action(context, Actions.DISMISS, step, 20))
            .setTimeoutAfter(ALARM_TIMEOUT_MS)
            .setAutoCancel(true)
            .addAction(0, "Gedaan", action(context, Actions.DONE, step, 21))
        if (step >= 0) builder.addAction(0, "Nog 5 min", action(context, Actions.SNOOZE, step, 22))
        val notification = builder.build().apply { flags = flags or Notification.FLAG_INSISTENT }
        post(context, ID_ALARM, notification)
    }

    fun cancelAlarm(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_ALARM)
    }

    fun showInfo(context: Context, title: String, text: String) {
        val n = NotificationCompat.Builder(context, CHANNEL_INFO)
            .setSmallIcon(R.drawable.ic_stat_bread)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .setTimeoutAfter(15 * MINUTE_MS)
            .build()
        post(context, ID_INFO, n)
    }

    /** Vaste melding zolang er een bak loopt: volgende stap, aftelling, Gedaan en Pauzeer. */
    fun updateOngoing(context: Context, bake: Bake?) {
        val nm = NotificationManagerCompat.from(context)
        if (bake == null) {
            nm.cancel(ID_ONGOING)
            return
        }
        val now = System.currentTimeMillis()
        val plan = Planner.plan(bake, now)
        val active = plan.active
        if (active == null) {
            nm.cancel(ID_ONGOING)
            return
        }
        val n = bake.recipe.steps.size
        val builder = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_bread)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openApp(context))
            .setProgress(n, plan.doneCount, false)
            .setSubText(bake.recipe.name)

        when {
            bake.pausedAt != null -> builder
                .setContentTitle("Bak op pauze")
                .setContentText("Volgende: ${active.step.name}")
                .addAction(0, "Hervat", action(context, Actions.RESUME, -1, 30))
            active.dueAt > now -> builder
                .setContentTitle("${active.step.name} om ${TimeFormat.clock(active.dueAt, now)}")
                .setContentText("Klaar om ${TimeFormat.clock(plan.finishAt, now)}")
                .setWhen(active.dueAt)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .addAction(0, "Gedaan", action(context, Actions.DONE, active.index, 31))
                .addAction(0, "Pauzeer", action(context, Actions.PAUSE, -1, 32))
            else -> builder
                .setContentTitle("Nu: ${active.step.name}")
                .setContentText("Tik op Gedaan als je klaar bent · klaar om ${TimeFormat.clock(plan.finishAt, now)}")
                .addAction(0, "Gedaan", action(context, Actions.DONE, active.index, 31))
                .addAction(0, "Pauzeer", action(context, Actions.PAUSE, -1, 32))
        }
        post(context, ID_ONGOING, builder.build())
    }

    private fun openApp(context: Context) = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun action(context: Context, action: String, step: Int, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, ActionReceiver::class.java).setAction(action).putExtra(Actions.EXTRA_STEP, step),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
