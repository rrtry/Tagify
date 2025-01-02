package com.rrtry.tagify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rrtry.tagify.prefs.prefsGetLastModifiedFile
import com.rrtry.tagify.service.ServiceEventBus
import com.rrtry.tagify.service.ServiceEventBus.Event.RestoreBackup
import com.rrtry.tagify.ui.theme.TagifyTheme
import com.rrtry.tagify.util.getBackup
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var serviceBus: ServiceEventBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TagifyTheme {
                TagifyApp(serviceBus)
            }
        }
        if (!isChangingConfigurations &&
            !serviceBus.inProgress.value)
        {
            getBackup(this)?.let { backupFile ->
                serviceBus.trySend(
                    RestoreBackup(backupFile, prefsGetLastModifiedFile(this)!!),
                )
            }
        }
    }
}