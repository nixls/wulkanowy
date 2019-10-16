package io.github.wulkanowy.data.repositories.preferences

import android.content.Context
import android.content.SharedPreferences
import io.github.wulkanowy.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val sharedPref: SharedPreferences,
    val context: Context
) {
    val startMenuIndex: Int
        get() = getString(R.string.pref_key_start_menu, R.string.pref_default_startup).toInt()

    val isShowPresent: Boolean
        get() = getBoolean(R.string.pref_key_attendance_present, R.bool.pref_default_attendance_present)

    val gradeAverageMode: String
        get() = getString(R.string.pref_key_grade_average_mode, R.string.pref_default_grade_average_mode)

    val gradeAverageForceCalc: Boolean
        get() = getBoolean(R.string.pref_key_grade_average_force_calc, R.bool.pref_default_grade_average_force_calc)

    val isGradeExpandable: Boolean
        get() = !getBoolean(R.string.pref_key_expand_grade, R.bool.pref_default_expand_grade)

    val appThemeKey = context.getString(R.string.pref_key_app_theme)
    val appTheme: String
        get() = getString(appThemeKey, R.string.pref_default_app_theme)

    val gradeColorTheme: String
        get() = getString(R.string.pref_key_grade_color_scheme, R.string.pref_default_grade_color_scheme)

    val serviceEnableKey = context.getString(R.string.pref_key_services_enable)
    val isServiceEnabled: Boolean
        get() = getBoolean(serviceEnableKey, R.bool.pref_default_services_enable)

    val servicesIntervalKey = context.getString(R.string.pref_key_services_interval)
    val servicesInterval: Long
        get() = getString(servicesIntervalKey, R.string.pref_default_services_interval).toLong()

    val servicesOnlyWifiKey = context.getString(R.string.pref_key_services_wifi_only)
    val isServicesOnlyWifi: Boolean
        get() = getBoolean(servicesOnlyWifiKey, R.bool.pref_default_services_wifi_only)

    val isNotificationsEnable: Boolean
        get() = getBoolean(R.string.pref_key_notifications_enable, R.bool.pref_default_notifications_enable)

    val isDebugNotificationEnableKey = context.getString(R.string.pref_key_notification_debug)
    val isDebugNotificationEnable: Boolean
        get() = getBoolean(isDebugNotificationEnableKey, R.bool.pref_default_notification_debug)

    val gradePlusModifier: Double
        get() = getString(R.string.pref_key_grade_modifier_plus, R.string.pref_default_grade_modifier_plus).toDouble()

    val gradeMinusModifier: Double
        get() = getString(R.string.pref_key_grade_modifier_minus, R.string.pref_default_grade_modifier_minus).toDouble()

    val fillMessageContent: Boolean
        get() = getBoolean(R.string.pref_key_fill_message_content, R.bool.pref_default_fill_message_content)

    private fun getString(id: Int, default: Int) = getString(context.getString(id), default)

    private fun getString(id: String, default: Int) = sharedPref.getString(id, context.getString(default)) ?: context.getString(default)

    private fun getBoolean(id: Int, default: Int) = getBoolean(context.getString(id), default)

    private fun getBoolean(id: String, default: Int) = sharedPref.getBoolean(id, context.resources.getBoolean(default))
}
