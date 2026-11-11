package com.dcfrederique.pizzabread.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dcfrederique.pizzabread.data.BakeStore
import com.dcfrederique.pizzabread.domain.MINUTE_MS
import com.dcfrederique.pizzabread.domain.Planner

/** Gaat af op het geplande moment: toont het alarm en plant het volgende. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        BakeStore.init(context)
        val bake = BakeStore.current.bake ?: return
        val now = System.currentTimeMillis()
        val at = intent.getLongExtra(AlarmScheduler.EXTRA_AT, now)

        if (intent.getStringExtra(AlarmScheduler.EXTRA_KIND) == AlarmScheduler.KIND_PAUSE) {
            if (bake.pausedAt != null) {
                Notifications.showInfo(context, "Je bak staat nog op pauze", bake.recipe.name)
            }
            return
        }

        val due = Planner.events(bake, now, after = at - 1000)
            .filter { it.at < maxOf(at, now) + MINUTE_MS / 2 }
        val ringing = due.filter { it.rings }
        if (ringing.isNotEmpty()) {
            Notifications.showAlarm(context, ringing)
        } else if (due.isNotEmpty()) {
            Notifications.showInfo(context, due.first().title, bake.recipe.name)
        }

        if (Planner.plan(bake, now).isFinished) {
            BakeStore.finish(aborted = false)
        } else {
            BakeStore.resync(after = maxOf(now, due.maxOfOrNull { it.at } ?: at))
        }
    }
}

/** Knoppen in de meldingen: Gedaan, Nog 5 min, Pauzeer, Hervat. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        BakeStore.init(context)
        Actions.handle(context, intent.action ?: return, intent.getIntExtra(Actions.EXTRA_STEP, -1))
    }
}

/** Na herstart, update of klokwijziging alle alarmen opnieuw registreren. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        BakeStore.init(context)
        BakeStore.resync()
    }
}

object Actions {
    const val DONE = "com.dcfrederique.pizzabread.DONE"
    const val SNOOZE = "com.dcfrederique.pizzabread.SNOOZE"
    const val PAUSE = "com.dcfrederique.pizzabread.PAUSE"
    const val RESUME = "com.dcfrederique.pizzabread.RESUME"
    const val DISMISS = "com.dcfrederique.pizzabread.DISMISS"
    const val EXTRA_STEP = "step"

    fun handle(context: Context, action: String, step: Int) {
        Notifications.cancelAlarm(context)
        when (action) {
            DONE -> if (step >= 0) BakeStore.update { b, now -> Planner.done(b, step, now) }
            SNOOZE -> if (step >= 0) BakeStore.update { b, now -> Planner.snooze(b, step, now) }
            PAUSE -> BakeStore.update { b, now -> Planner.pause(b, now) }
            RESUME -> BakeStore.update { b, now -> Planner.resume(b, now) }
            DISMISS -> Unit
        }
        val bake = BakeStore.current.bake ?: return
        if (Planner.plan(bake, System.currentTimeMillis()).isFinished) BakeStore.finish(aborted = false)
    }
}
