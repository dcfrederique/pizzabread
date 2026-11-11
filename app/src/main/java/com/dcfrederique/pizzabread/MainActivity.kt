package com.dcfrederique.pizzabread

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dcfrederique.pizzabread.data.BakeStore
import com.dcfrederique.pizzabread.ui.App
import com.dcfrederique.pizzabread.ui.BroodTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BakeStore.init(this)
        setContent { BroodTheme { App() } }
    }

    override fun onResume() {
        super.onResume()
        // Na een gewijzigd recht of een gemiste herstart: alarmen en vaste melding herstellen.
        BakeStore.resync()
    }
}
