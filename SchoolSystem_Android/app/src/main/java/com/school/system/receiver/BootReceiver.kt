package com.school.system.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.school.system.data.BellManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var bellManager: BellManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                bellManager.scheduleAllAlarms()
            }
        }
    }
}
