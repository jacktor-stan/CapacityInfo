package com.ph03nix_x.capacityinfo

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.preference.PreferenceManager
import com.ph03nix_x.capacityinfo.services.CapacityInfoService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private var jobStartService: Job? = null
interface ServiceInterface {

    fun startService(context: Context) {

        val pref = PreferenceManager.getDefaultSharedPreferences(context)

        if(!isStartedJob())
        jobStartService = GlobalScope.launch {
            delay(1000)
            if(pref.getBoolean(Preferences.IsEnableService.prefKey, true)) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context.startForegroundService(Intent(context, CapacityInfoService::class.java))
                else context.startService(Intent(context, CapacityInfoService::class.java))
            }
            jobStartService = null
        }
    }

    private fun isStartedJob() = jobStartService != null
}