package io.github.wulkanowy.services.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.AlarmManagerCompat
import io.github.wulkanowy.data.db.entities.Timetable
import io.github.wulkanowy.data.repositories.preferences.PreferencesRepository
import io.github.wulkanowy.services.alarm.AlarmBroadcastReceiver.Companion.LESSON_END
import io.github.wulkanowy.services.alarm.AlarmBroadcastReceiver.Companion.LESSON_ROOM
import io.github.wulkanowy.services.alarm.AlarmBroadcastReceiver.Companion.LESSON_START
import io.github.wulkanowy.services.alarm.AlarmBroadcastReceiver.Companion.LESSON_TITLE
import io.github.wulkanowy.services.alarm.AlarmBroadcastReceiver.Companion.LESSON_TYPE
import io.github.wulkanowy.services.alarm.AlarmBroadcastReceiver.Companion.NOTIFICATION_TYPE_CURRENT
import io.github.wulkanowy.services.alarm.AlarmBroadcastReceiver.Companion.NOTIFICATION_TYPE_UPCOMING
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalDateTime.now
import org.threeten.bp.ZoneId
import timber.log.Timber
import javax.inject.Inject

class AlarmHelper @Inject constructor(
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val preferencesRepository: PreferencesRepository
) {

    private fun LocalDateTime.toTimestamp() = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun Long.toLocalDateTime() = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

    fun cancelNotifications(lessons: List<Timetable>, studentId: Int = 1) {
        lessons.sortedBy { it.start }.forEachIndexed { index, lesson ->
            val upcomingTimestamp = lessons.getOrNull(index - 1)?.end?.toTimestamp() ?: lesson.start.minusMinutes(30).toTimestamp()
            val currentTimestamp = lesson.start.toTimestamp()
            alarmManager.cancel(PendingIntent.getBroadcast(context, (upcomingTimestamp * studentId).toInt(), Intent(), PendingIntent.FLAG_CANCEL_CURRENT))
            alarmManager.cancel(PendingIntent.getBroadcast(context, (currentTimestamp * studentId).toInt(), Intent(), PendingIntent.FLAG_CANCEL_CURRENT))
        }
        Timber.d("Timetable notifications canceled")
    }

    fun scheduleNotifications(lessons: List<Timetable>, studentId: Int = 1) {
        if (!preferencesRepository.isUpcomingLessonsNotificationsEnable) return cancelNotifications(lessons, studentId)

        Timber.d("${lessons.size} to schedule, current millis: ${now().toTimestamp()}")
        lessons.sortedBy { it.start }.forEachIndexed { index, lesson ->
            val intent = with(Intent(context, AlarmBroadcastReceiver::class.java)) {
                putExtra(LESSON_ROOM, lesson.room)
                putExtra(LESSON_START, lesson.start.toTimestamp())
                putExtra(LESSON_END, lesson.end.toTimestamp())
                putExtra(LESSON_TITLE, lesson.subject)
            }

            scheduleUpcoming(lessons, lesson, index, intent, studentId)
            scheduleCurrent(lesson, intent, studentId)
        }
        Timber.d("Timetable notifications scheduled")
    }

    private fun scheduleUpcoming(lessons: List<Timetable>, lesson: Timetable, index: Int, intent: Intent, studentId: Int) {
        if (lesson.start >= now()) {
            Timber.d("${lesson.start} >= ${now()}")
            val upcomingTimestamp = lessons.getOrNull(index - 1)?.end?.toTimestamp() ?: lesson.start.minusMinutes(30).toTimestamp()
            AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, upcomingTimestamp,
                PendingIntent.getBroadcast(context, (upcomingTimestamp * studentId).toInt(), intent.also { it.putExtra(LESSON_TYPE, NOTIFICATION_TYPE_UPCOMING) }, PendingIntent.FLAG_CANCEL_CURRENT)
            )
            Timber.d("- Scheduled lesson as upcoming at ${upcomingTimestamp.toLocalDateTime()} (${lessons.getOrNull(index - 1)?.end ?: lesson.start.minusMinutes(30)}) $lesson")
        } else Timber.d("Not scheduled")
    }

    private fun scheduleCurrent(lesson: Timetable, intent: Intent, studentId: Int) {
        if (lesson.end > now()) {
            Timber.d("${lesson.end} > ${now()}")
            val currentTimestamp = lesson.start.toTimestamp()
            AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, currentTimestamp,
                PendingIntent.getBroadcast(context, (currentTimestamp * studentId).toInt(), intent.also { it.putExtra(LESSON_TYPE, NOTIFICATION_TYPE_CURRENT) }, PendingIntent.FLAG_CANCEL_CURRENT)
            )
            Timber.d("- Scheduled lesson as current at ${currentTimestamp.toLocalDateTime()} (${lesson.start}) $lesson")
        } else Timber.d("Not scheduled")
    }
}
