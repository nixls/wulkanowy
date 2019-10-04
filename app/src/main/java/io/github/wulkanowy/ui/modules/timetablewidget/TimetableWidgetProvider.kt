package io.github.wulkanowy.ui.modules.timetablewidget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_DELETED
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.widget.RemoteViews
import dagger.android.AndroidInjection
import io.github.wulkanowy.R
import io.github.wulkanowy.data.db.SharedPrefProvider
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.exceptions.NoCurrentStudentException
import io.github.wulkanowy.data.repositories.student.StudentRepository
import io.github.wulkanowy.services.widgets.TimetableWidgetService
import io.github.wulkanowy.ui.modules.main.MainActivity
import io.github.wulkanowy.ui.modules.main.MainView
import io.github.wulkanowy.utils.FirebaseAnalyticsHelper
import io.github.wulkanowy.utils.SchedulersProvider
import io.github.wulkanowy.utils.nextOrSameSchoolDay
import io.github.wulkanowy.utils.nextSchoolDay
import io.github.wulkanowy.utils.previousSchoolDay
import io.github.wulkanowy.utils.shortcutWeekDayName
import io.github.wulkanowy.utils.toFormattedString
import io.reactivex.Maybe
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDate.now
import timber.log.Timber
import javax.inject.Inject

class TimetableWidgetProvider : BroadcastReceiver() {

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    @Inject
    lateinit var studentRepository: StudentRepository

    @Inject
    lateinit var sharedPref: SharedPrefProvider

    @Inject
    lateinit var schedulers: SchedulersProvider

    @Inject
    lateinit var analytics: FirebaseAnalyticsHelper

    companion object {

        private const val EXTRA_TOGGLED_WIDGET_ID = "extraToggledWidget"

        private const val EXTRA_BUTTON_TYPE = "extraButtonType"

        private const val BUTTON_NEXT = "buttonNext"

        private const val BUTTON_PREV = "buttonPrev"

        private const val BUTTON_RESET = "buttonReset"

        const val EXTRA_FROM_PROVIDER = "extraFromProvider"

        fun getDateWidgetKey(appWidgetId: Int) = "timetable_widget_date_$appWidgetId"

        fun getStudentWidgetKey(appWidgetId: Int) = "timetable_widget_student_$appWidgetId"

        fun getThemeWidgetKey(appWidgetId: Int) = "timetable_widget_theme_$appWidgetId"
    }

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)
        when (intent.action) {
            ACTION_APPWIDGET_UPDATE -> onUpdate(context, intent)
            ACTION_APPWIDGET_DELETED -> onDelete(intent)
        }
    }

    private fun onUpdate(context: Context, intent: Intent) {
        if (intent.getStringExtra(EXTRA_BUTTON_TYPE) === null) {
            intent.getIntArrayExtra(EXTRA_APPWIDGET_IDS)?.forEach { appWidgetId ->
                val student = getStudent(sharedPref.getLong(getStudentWidgetKey(appWidgetId), 0), appWidgetId)
                updateWidget(context, appWidgetId, now().nextOrSameSchoolDay, student)
            }
        } else {
            val buttonType = intent.getStringExtra(EXTRA_BUTTON_TYPE)
            val toggledWidgetId = intent.getIntExtra(EXTRA_TOGGLED_WIDGET_ID, 0)
            val student = getStudent(sharedPref.getLong(getStudentWidgetKey(toggledWidgetId), 0), toggledWidgetId)
            val savedDate = LocalDate.ofEpochDay(sharedPref.getLong(getDateWidgetKey(toggledWidgetId), 0))
            val date = when (buttonType) {
                BUTTON_RESET -> now().nextOrSameSchoolDay
                BUTTON_NEXT -> savedDate.nextSchoolDay
                BUTTON_PREV -> savedDate.previousSchoolDay
                else -> now().nextOrSameSchoolDay
            }
            if (!buttonType.isNullOrBlank()) analytics.logEvent("changed_timetable_widget_day", "button" to buttonType)
            updateWidget(context, toggledWidgetId, date, student)
        }
    }

    private fun onDelete(intent: Intent) {
        val appWidgetId = intent.getIntExtra(EXTRA_APPWIDGET_ID, 0)

        if (appWidgetId != 0) {
            with(sharedPref) {
                delete(getStudentWidgetKey(appWidgetId))
                delete(getDateWidgetKey(appWidgetId))
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun updateWidget(context: Context, appWidgetId: Int, date: LocalDate, student: Student?) {
        val savedTheme = sharedPref.getLong(getThemeWidgetKey(appWidgetId), 0)
        val layoutId = if (savedTheme == 0L) R.layout.widget_timetable else R.layout.widget_timetable_dark

        val nextNavIntent = createNavIntent(context, appWidgetId, appWidgetId, BUTTON_NEXT)
        val prevNavIntent = createNavIntent(context, -appWidgetId, appWidgetId, BUTTON_PREV)
        val resetNavIntent = createNavIntent(context, Int.MAX_VALUE - appWidgetId, appWidgetId, BUTTON_RESET)
        val adapterIntent = Intent(context, TimetableWidgetService::class.java)
            .apply {
                putExtra(EXTRA_APPWIDGET_ID, appWidgetId)
                //make Intent unique
                action = appWidgetId.toString()
            }
        val accountIntent = PendingIntent.getActivity(context, -Int.MAX_VALUE + appWidgetId,
            Intent(context, TimetableWidgetConfigureActivity::class.java).apply {
                addFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(EXTRA_FROM_PROVIDER, true)
            }, FLAG_UPDATE_CURRENT)
        val appIntent = PendingIntent.getActivity(context, MainView.Section.TIMETABLE.id,
            MainActivity.getStartIntent(context, MainView.Section.TIMETABLE, true), FLAG_UPDATE_CURRENT)

        val remoteView = RemoteViews(context.packageName, layoutId).apply {
            setEmptyView(R.id.timetableWidgetList, R.id.timetableWidgetEmpty)
            setTextViewText(R.id.timetableWidgetDate, date.toFormattedString("EEEE, dd.MM").capitalize())
            setTextViewText(R.id.timetableWidgetName, student?.studentName ?: context.getString(R.string.all_no_data))
            setRemoteAdapter(R.id.timetableWidgetList, adapterIntent)
            setOnClickPendingIntent(R.id.timetableWidgetNext, nextNavIntent)
            setOnClickPendingIntent(R.id.timetableWidgetPrev, prevNavIntent)
            setOnClickPendingIntent(R.id.timetableWidgetDate, resetNavIntent)
            setOnClickPendingIntent(R.id.timetableWidgetName, resetNavIntent)
            setOnClickPendingIntent(R.id.timetableWidgetAccount, accountIntent)
            setPendingIntentTemplate(R.id.timetableWidgetList, appIntent)
        }

        sharedPref.putLong(getDateWidgetKey(appWidgetId), date.toEpochDay(), true)
        with(appWidgetManager) {
            notifyAppWidgetViewDataChanged(appWidgetId, R.id.timetableWidgetList)
            updateAppWidget(appWidgetId, remoteView)
        }
    }

    private fun createNavIntent(context: Context, code: Int, appWidgetId: Int, buttonType: String): PendingIntent {
        return PendingIntent.getBroadcast(context, code,
            Intent(context, TimetableWidgetProvider::class.java).apply {
                action = ACTION_APPWIDGET_UPDATE
                putExtra(EXTRA_BUTTON_TYPE, buttonType)
                putExtra(EXTRA_TOGGLED_WIDGET_ID, appWidgetId)
            }, FLAG_UPDATE_CURRENT)
    }

    private fun getStudent(studentId: Long, appWidgetId: Int): Student? {
        return try {
            studentRepository.isStudentSaved()
                .filter { true }
                .flatMap { studentRepository.getSavedStudents(false).toMaybe() }
                .flatMap { students ->
                    val student = students.singleOrNull { student -> student.id == studentId }
                    when {
                        student != null -> Maybe.just(student)
                        studentId != 0L -> {
                            studentRepository.isCurrentStudentSet()
                                .filter { true }
                                .flatMap { studentRepository.getCurrentStudent(false).toMaybe() }
                                .doOnSuccess { sharedPref.putLong(getStudentWidgetKey(appWidgetId), it.id) }
                        }
                        else -> Maybe.empty()
                    }
                }
                .subscribeOn(schedulers.backgroundThread)
                .blockingGet()
        } catch (e: Exception) {
            if (e.cause !is NoCurrentStudentException) {
                Timber.e(e, "An error has occurred in timetable widget provider")
            }
            null
        }
    }
}
