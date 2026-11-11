package com.dcfrederique.pizzabread.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcfrederique.pizzabread.data.BakeStore
import com.dcfrederique.pizzabread.domain.TimeFormat
import com.dcfrederique.pizzabread.ui.BroodTheme

/** Volledig-scherm-alarm, ook op het vergrendelscherm. */
class AlarmActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_TEXT = "text"
        private const val EXTRA_STEP = "step"

        fun intent(context: Context, title: String, text: String, step: Int) =
            Intent(context, AlarmActivity::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_TEXT, text)
                .putExtra(EXTRA_STEP, step)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BakeStore.init(this)
        showOverLockScreen()
        render(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        render(intent)
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun render(intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        val step = intent.getIntExtra(EXTRA_STEP, -1)
        val now = System.currentTimeMillis()
        val bakeName = BakeStore.current.bake?.recipe?.name.orEmpty()

        setContent {
            BroodTheme {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(TimeFormat.clock(now, now), fontSize = 56.sp, fontWeight = FontWeight.Light)
                        Text(bakeName, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(40.dp))
                        Text(title, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 38.sp)
                        if (text.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                        }
                        Spacer(Modifier.height(56.dp))
                        Button(
                            onClick = { act(Actions.DONE, step) },
                            modifier = Modifier.fillMaxWidth().height(72.dp),
                        ) { Text("Gedaan", fontSize = 24.sp) }
                        if (step >= 0) {
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { act(Actions.SNOOZE, step) },
                                modifier = Modifier.fillMaxWidth().height(64.dp),
                            ) { Text("Nog 5 min", fontSize = 20.sp) }
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { act(Actions.DISMISS, step) }) {
                                Text("Alarm stil, ik ben bezig")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun act(action: String, step: Int) {
        Actions.handle(this, action, step)
        finish()
    }
}
