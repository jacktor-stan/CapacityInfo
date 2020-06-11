package com.ph03nix_x.capacityinfo.activities

import android.content.*
import android.net.Uri
import android.os.BatteryManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ph03nix_x.capacityinfo.*
import com.ph03nix_x.capacityinfo.MainApp.Companion.defLang
import com.ph03nix_x.capacityinfo.fragments.*
import com.ph03nix_x.capacityinfo.MainApp.Companion.batteryIntent
import com.ph03nix_x.capacityinfo.services.*
import com.ph03nix_x.capacityinfo.views.CenteredToolbar
import com.ph03nix_x.capacityinfo.helpers.LocaleHelper
import com.ph03nix_x.capacityinfo.interfaces.BatteryInfoInterface
import com.ph03nix_x.capacityinfo.interfaces.OverlayInterface
import com.ph03nix_x.capacityinfo.helpers.ServiceHelper
import com.ph03nix_x.capacityinfo.interfaces.SettingsInterface
import com.ph03nix_x.capacityinfo.utilities.Constants.DONT_KILL_MY_APP_LINK
import com.ph03nix_x.capacityinfo.utilities.Constants.IMPORT_SETTINGS_EXTRA
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.BATTERY_LEVEL_TO
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.BATTERY_LEVEL_WITH
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.CAPACITY_ADDED
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.DESIGN_CAPACITY
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.IS_ENABLED_DEBUG_OPTIONS
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.IS_SHOW_INSTRUCTION
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.IS_SHOW_NOT_SUPPORTED_DIALOG
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.IS_SUPPORTED
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.LANGUAGE
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.LAST_CHARGE_TIME
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.NUMBER_OF_CHARGES
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.NUMBER_OF_CYCLES
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.PERCENT_ADDED
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.RESIDUAL_CAPACITY
import com.ph03nix_x.capacityinfo.utilities.PreferencesKeys.TAB_ON_APPLICATION_LAUNCH

class MainActivity : AppCompatActivity(), BatteryInfoInterface, SettingsInterface {

    private lateinit var pref: SharedPreferences
    private var isDoubleBackToExitPressedOnce = false
    private var prefArrays: HashMap<*, *>? = null

    lateinit var toolbar: CenteredToolbar
    lateinit var navigation: BottomNavigationView

    var fragment: Fragment? = null

    companion object {

        var instance: MainActivity? = null
        var isLoadChargeDischarge = false
        var isLoadWear = false
        var isLoadSettings = false
        var isLoadDebug = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        pref = PreferenceManager.getDefaultSharedPreferences(this)

        super.onCreate(savedInstanceState)

        LocaleHelper.setLocale(this, pref.getString(
            LANGUAGE, null) ?: defLang)

        setContentView(R.layout.activity_main)

        batteryIntent = registerReceiver(null, IntentFilter(
            Intent.ACTION_BATTERY_CHANGED))

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN) ?: BatteryManager.BATTERY_STATUS_UNKNOWN

        toolbar = findViewById(R.id.toolbar)

        navigation = findViewById(R.id.navigation)

        prefArrays = intent.getSerializableExtra(IMPORT_SETTINGS_EXTRA)
                as? HashMap<*, *>

        fragment = when {

            isLoadChargeDischarge || (pref.getString(TAB_ON_APPLICATION_LAUNCH, "0") != "1"
                    && prefArrays == null && !isLoadSettings && !isLoadDebug) ->
                ChargeDischargeFragment()

            isLoadWear || (pref.getString(TAB_ON_APPLICATION_LAUNCH, "0") == "1" &&
                    prefArrays == null && !isLoadSettings && !isLoadDebug) -> WearFragment()

            isLoadDebug -> DebugFragment()

            else -> SettingsFragment()
        }

        toolbar.title = when(fragment) {

            is ChargeDischargeFragment -> getString(
                if(status == BatteryManager.BATTERY_STATUS_CHARGING) R.string.charge
                else R.string.discharge)

            is WearFragment -> getString(R.string.wear)
            is SettingsFragment -> getString(R.string.settings)
            is DebugFragment -> getString(R.string.debug)
            else -> getString(R.string.app_name)
        }

        toolbar.navigationIcon = null

        if(fragment !is SettingsFragment && fragment !is DebugFragment) inflateMenu()

        toolbar.setNavigationOnClickListener {

            onBackPressed()
        }

        navigation.menu.findItem(R.id.charge_discharge_navigation).title = getString(
            if(status == BatteryManager.BATTERY_STATUS_CHARGING) R.string.charging
            else R.string.discharge)

        navigation.menu.findItem(R.id.charge_discharge_navigation).icon = getDrawable(
            getChargeDischargeNavigationIcon(status ==
                    BatteryManager.BATTERY_STATUS_CHARGING))

        navigation.menu.findItem(R.id.debug_navigation).isVisible =
            pref.getBoolean(IS_ENABLED_DEBUG_OPTIONS, resources.getBoolean(
                R.bool.is_enabled_debug_options))

        navigation.setOnNavigationItemSelectedListener {

            when(it.itemId) {

                R.id.charge_discharge_navigation -> {

                    if(fragment !is ChargeDischargeFragment) {

                        toolbar.navigationIcon = null

                        isLoadChargeDischarge = true

                        isLoadWear = false

                        isLoadSettings = false

                        isLoadDebug = false

                        clearMenu()

                        inflateMenu()

                        loadFragment(fragment ?: ChargeDischargeFragment())
                    }
                }

                R.id.wear_navigation -> {

                    if(fragment !is WearFragment) {

                        toolbar.title = getString(R.string.wear)

                        toolbar.navigationIcon = null

                        isLoadChargeDischarge = false

                        isLoadWear = true

                        isLoadSettings = false

                        isLoadDebug = false

                        clearMenu()

                        inflateMenu()

                        fragment = WearFragment()

                        loadFragment(fragment ?: WearFragment())
                    }
                }

                R.id.settings_navigation -> {

                    when(fragment) {

                        null, is ChargeDischargeFragment, is WearFragment, is DebugFragment -> {

                            toolbar.title = getString(R.string.settings)

                            toolbar.navigationIcon = null

                            isLoadChargeDischarge = false

                            isLoadWear = false

                            isLoadSettings = true

                            isLoadDebug = false

                            clearMenu()

                            fragment = SettingsFragment()

                            loadFragment(fragment ?: SettingsFragment())
                        }
                    }
                }

                R.id.debug_navigation -> {

                    if(fragment !is DebugFragment) {

                        toolbar.title = getString(R.string.debug)

                        toolbar.navigationIcon = null

                        isLoadChargeDischarge = false

                        isLoadWear = false

                        isLoadSettings = false

                        isLoadDebug = true

                        clearMenu()

                        fragment = DebugFragment()

                        loadFragment(fragment ?: DebugFragment())
                    }
                }
            }

            true
        }

        loadFragment(fragment ?: ChargeDischargeFragment())
    }

    override fun onResume() {

        super.onResume()

        if(instance == null) instance = this

        batteryIntent = registerReceiver(null, IntentFilter(
            Intent.ACTION_BATTERY_CHANGED))

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN) ?: BatteryManager.BATTERY_STATUS_UNKNOWN

        if(fragment !is ChargeDischargeFragment) {

            navigation.menu.findItem(R.id.charge_discharge_navigation).title = getString(if(
                status == BatteryManager.BATTERY_STATUS_CHARGING) R.string.charge else
                R.string.discharge)

            navigation.menu.findItem(R.id.charge_discharge_navigation).icon = getDrawable(
                getChargeDischargeNavigationIcon(status == BatteryManager
                    .BATTERY_STATUS_CHARGING))
        }

        if(CapacityInfoService.instance == null && !ServiceHelper.isStartedService())
            ServiceHelper.startService(this, CapacityInfoService::class.java,
                true)

        if(!pref.getBoolean(IS_ENABLED_DEBUG_OPTIONS, resources.getBoolean(
                R.bool.is_enabled_debug_options))
            && fragment is DebugFragment) {

            fragment = ChargeDischargeFragment()
            loadFragment(fragment ?: ChargeDischargeFragment())
        }

        toolbar.title = when(fragment) {

            is ChargeDischargeFragment -> getString(
                if(status == BatteryManager.BATTERY_STATUS_CHARGING) R.string.charge
                else R.string.discharge)

            is WearFragment -> getString(R.string.wear)
            is SettingsFragment -> getString(R.string.settings)
            is BatteryStatusInformationFragment -> getString(R.string.battery_status_information)
            is OverlayFragment -> getString(R.string.overlay)
            is AboutFragment -> getString(R.string.about)
            is FeedbackFragment -> getString(R.string.feedback)
            is DebugFragment -> getString(R.string.debug)
            else -> getString(R.string.app_name)
        }

        navigation.menu.findItem(R.id.debug_navigation).isVisible =
            pref.getBoolean(IS_ENABLED_DEBUG_OPTIONS, resources.getBoolean(
                R.bool.is_enabled_debug_options))

        if(pref.getInt(DESIGN_CAPACITY, resources.getInteger(R.integer.min_design_capacity)) <
            resources.getInteger(R.integer.min_design_capacity) || pref.getInt(DESIGN_CAPACITY,
                resources.getInteger(R.integer.min_design_capacity)) > resources.getInteger(
                R.integer.max_design_capacity)) {

            pref.edit().apply {

                putInt(DESIGN_CAPACITY, getOnDesignCapacity(this@MainActivity))

                apply()
            }
        }

        if(getOnCurrentCapacity(this) == 0.0
            && pref.getBoolean(IS_SHOW_NOT_SUPPORTED_DIALOG, resources.getBoolean(
                R.bool.is_show_not_supported_dialog))) {

            pref.edit().putBoolean(IS_SHOW_NOT_SUPPORTED_DIALOG, false).apply()

            pref.edit().putBoolean(IS_SUPPORTED, false).apply()

            MaterialAlertDialogBuilder(this).apply {

                setIcon(R.drawable.ic_instruction_not_supported_24dp)
                setTitle(getString(R.string.information))
                setMessage(getString(R.string.not_supported))
                setPositiveButton(android.R.string.ok) { d, _ -> d.dismiss() }
                setCancelable(false)
                show()
            }
        }

        else if(pref.getBoolean(IS_SUPPORTED, resources.getBoolean(R.bool.is_supported))
            && pref.getBoolean(IS_SHOW_INSTRUCTION, resources.getBoolean(
                R.bool.is_show_instruction))) showInstruction()

        if(fragment is ChargeDischargeFragment || fragment is WearFragment)
            toolbar.menu.findItem(R.id.instruction).isVisible = getOnCurrentCapacity(
                this) > 0.0

        val prefArrays = intent.getSerializableExtra(IMPORT_SETTINGS_EXTRA)
                as? HashMap<*, *>
        if(prefArrays != null) importSettings(prefArrays)

        if(OverlayService.instance == null && OverlayInterface.isEnabledOverlay(this))
            ServiceHelper.startService(this, OverlayService::class.java)
    }

    override fun onBackPressed() {

        if(toolbar.title != getString(R.string.settings) && fragment != null
            && fragment !is SettingsFragment && fragment !is ChargeDischargeFragment
            && fragment !is WearFragment && fragment !is DebugFragment) {

            fragment = SettingsFragment()

            toolbar.title = getString(R.string.settings)

            toolbar.navigationIcon = null

            super.onBackPressed()
        }

        else {

            if(isDoubleBackToExitPressedOnce) finish()

            else if(!isDoubleBackToExitPressedOnce) {

                isDoubleBackToExitPressedOnce = true

                Toast.makeText(this, R.string.press_the_back_button_again,
                    Toast.LENGTH_LONG).show()

                Handler().postDelayed({ isDoubleBackToExitPressedOnce = false }, 3000)
            }
        }
    }

    override fun onDestroy() {

        fragment = null

        instance = null

        isLoadChargeDischarge = false
        isLoadWear = false
        isLoadSettings = false
        isLoadDebug = false

        super.onDestroy()
    }

    private fun inflateMenu() {

        toolbar.inflateMenu(R.menu.main_menu)

        toolbar.menu.findItem(R.id.instruction).isVisible = getOnCurrentCapacity(
            this) > 0.0 && (fragment is ChargeDischargeFragment || fragment is WearFragment)

        toolbar.menu.findItem(R.id.instruction).setOnMenuItemClickListener {

            showInstruction()

            true
        }

        toolbar.menu.findItem(R.id.faq).setOnMenuItemClickListener {

            MaterialAlertDialogBuilder(this).apply {

                setIcon(R.drawable.ic_faq_question_24dp)
                setTitle(getString(R.string.faq))
                setMessage(getString(R.string.faq_how_does_the_app_work)
                        + getString(R.string.faq_capacity_added)
                        + getString(R.string.faq_i_have_everything_in_zeros)
                        + getString(R.string.faq_units) + getString(R.string.faq_current_capacity)
                        + getString(R.string.faq_add_device_support))
                setPositiveButton(android.R.string.ok) { d, _ -> d.dismiss() }
                show()
            }

            true
        }

        toolbar.menu.findItem(R.id.tips).setOnMenuItemClickListener {

            MaterialAlertDialogBuilder(this).apply {

                setIcon(R.drawable.ic_tips_for_extending_battery_life_24dp)
                setTitle(getString(R.string.tips_dialog_title))
                setMessage(getString(R.string.tip1) + getString(R.string.tip2)
                        + getString(R.string.tip3) + getString(R.string.tip4)
                        + getString(R.string.tip5) + getString(R.string.tip6)
                        + getString(R.string.tip7))
                setPositiveButton(android.R.string.ok) { d, _ -> d.dismiss() }
                show()
            }

            true
        }

        toolbar.menu.findItem(R.id.dont_kill_my_app).setOnMenuItemClickListener {

            try {

                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DONT_KILL_MY_APP_LINK)))
            }
            catch(e: ActivityNotFoundException) {

                Toast.makeText(this, e.message ?: e.toString(), Toast.LENGTH_LONG)
                    .show()
            }

            true
        }
    }

    private fun clearMenu() = toolbar.menu.clear()

    private fun showInstruction() {

        MaterialAlertDialogBuilder(this).apply {

            if(pref.getBoolean(IS_SHOW_INSTRUCTION, resources.getBoolean(
                    R.bool.is_show_instruction)))
                pref.edit().putBoolean(IS_SHOW_INSTRUCTION, false).apply()

            setIcon(R.drawable.ic_instruction_not_supported_24dp)
            setTitle(getString(R.string.instruction))
            setMessage(getString(R.string.instruction_message)
                    + getString(R.string.instruction_message_do_not_kill_the_service)
                    + getString(R.string.instruction_message_dont_kill_my_app)
                    + getString(R.string.instruction_message_huawei_honor))

            setPositiveButton(android.R.string.ok) { d, _ -> d.dismiss() }

            setCancelable(false)

            show()
        }
    }

    fun loadFragment(fragment: Fragment, isAddToBackStack: Boolean = false) {

        supportFragmentManager.beginTransaction().apply {

            replace(R.id.fragment_container, fragment)
            if(isAddToBackStack) addToBackStack(null)
            commit()
        }

        when {

            fragment !is BatteryStatusInformationFragment && fragment !is OverlayFragment
                    && fragment !is AboutFragment && fragment !is FeedbackFragment -> {

                navigation.selectedItemId = when(fragment) {

                    is ChargeDischargeFragment -> R.id.charge_discharge_navigation
                    is WearFragment -> R.id.wear_navigation
                    is SettingsFragment -> R.id.settings_navigation
                    is DebugFragment -> R.id.debug_navigation
                    else -> R.id.charge_discharge_navigation
                }
            }
        }
    }

    fun getChargeDischargeNavigationIcon(isCharge: Boolean): Int {

        val batteryLevel = getOnBatteryLevel(this) ?: 0

        if(isCharge)
            return when(batteryLevel) {

            in 0..29 -> R.drawable.ic_charge_navigation_20_24dp
            in 30..49 -> R.drawable.ic_charge_navigation_30_24dp
            in 50..59 -> R.drawable.ic_charge_navigation_50_24dp
            in 60..79 -> R.drawable.ic_charge_navigation_60_24dp
            in 80..89 -> R.drawable.ic_charge_navigation_80_24dp
            in 90..95 -> R.drawable.ic_charge_navigation_90_24dp
            else -> R.drawable.ic_charge_navigation_full_24dp
        }

        else return when(batteryLevel) {

            in 0..9 -> R.drawable.ic_discharge_navigation_9_24dp
            in 10..29 -> R.drawable.ic_discharge_navigation_20_24dp
            in 30..49 -> R.drawable.ic_discharge_navigation_30_24dp
            in 50..59 -> R.drawable.ic_discharge_navigation_50_24dp
            in 60..79 -> R.drawable.ic_discharge_navigation_60_24dp
            in 80..89 -> R.drawable.ic_discharge_navigation_80_24dp
            in 90..95 -> R.drawable.ic_discharge_navigation_90_24dp
            else -> R.drawable.ic_discharge_navigation_full_24dp
        }
    }

    private fun importSettings(prefArrays: HashMap<*, *>?) {

        val prefsTempList = arrayListOf(BATTERY_LEVEL_TO, BATTERY_LEVEL_WITH,
            DESIGN_CAPACITY, CAPACITY_ADDED, LAST_CHARGE_TIME, PERCENT_ADDED, RESIDUAL_CAPACITY,
            IS_SUPPORTED, IS_SHOW_NOT_SUPPORTED_DIALOG, IS_SHOW_INSTRUCTION)

        if(prefArrays != null)
        prefsTempList.forEach {

            with(prefArrays) {

                when {

                    !containsKey(it) -> pref.edit().remove(it).apply()

                    else -> {

                        forEach {

                            when(it.key as String) {

                                NUMBER_OF_CHARGES -> pref.edit().putLong(it.key as String,
                                    it.value as Long).apply()

                                BATTERY_LEVEL_TO, BATTERY_LEVEL_WITH, LAST_CHARGE_TIME,
                                DESIGN_CAPACITY, RESIDUAL_CAPACITY, PERCENT_ADDED ->
                                    pref.edit().putInt(it.key as String, it.value as Int).apply()

                                CAPACITY_ADDED, NUMBER_OF_CYCLES ->
                                    pref.edit().putFloat(it.key as String,
                                        it.value as Float).apply()

                                IS_SUPPORTED, IS_SHOW_NOT_SUPPORTED_DIALOG, IS_SHOW_INSTRUCTION ->
                                    pref.edit().putBoolean(it.key as String,
                                        it.value as Boolean).apply()
                            }
                        }
                    }
                }
            }
        }

        toolbar.menu.clear()

        Toast.makeText(this, R.string.settings_imported_successfully,
            Toast.LENGTH_LONG).show()

        intent.removeExtra(IMPORT_SETTINGS_EXTRA)
    }
}