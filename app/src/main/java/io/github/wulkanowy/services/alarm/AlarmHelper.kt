package io.github.wulkanowy.services.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.AlarmManagerCompat
import io.github.wulkanowy.data.db.entities.Timetable
import io.github.wulkanowy.data.repositories.preferences.PreferencesRepository
import io.github.wulkanowy.services.alarm.AlarmBroadcastReceiver.Companion.LESSON_END
import io.github.wulkanowy.services.alarm.AlarmBroadcastReceiver.Companion.LESSON_NEXT_ROOM
import io.github.wulkanowy.services.alarm.AlarmBroadcastReceiver.Companion.LESSON_NEXT_TITLE
import io.github.wulkanowy.services.alarm.AlarmBroadcastReceiver.Companion.LESSON_ROOM
import io.github.wulkanowy.services.alarm.AlarmBroadcastReceiver.Companion.LESSON_START
import io.github.wulkanowy.services.alarm.AlarmBroadcastReceiver.Companion.LESSON_TITLE
import io.github.wulkanowy.services.alarm.AlarmBroadcastReceiver.Companion.LESSON_TYPE
import io.github.wulkanowy.services.alarm.AlarmBroadcastReceiver.Companion.NOTIFICATION_TYPE_CURRENT
import io.github.wulkanowy.services.alarm.AlarmBroadcastReceiver.Companion.NOTIFICATION_TYPE_LAST_LESSON_CANCELLATION
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
        lessons.groupBy { it.date }.map { week -> week.value.sortedBy { it.date } }.map { day ->
            val numberOfLessons = day.size
            day.forEachIndexed { index, lesson ->
                val intent = with(Intent(context, AlarmBroadcastReceiver::class.java)) {
                    putExtra(LESSON_ROOM, lesson.room)
                    putExtra(LESSON_START, lesson.start.toTimestamp())
                    putExtra(LESSON_END, lesson.end.toTimestamp())
                    putExtra(LESSON_TITLE, lesson.subject)
                    putExtra(LESSON_NEXT_TITLE, day.getOrNull(index + 1)?.subject)
                    putExtra(LESSON_NEXT_ROOM, day.getOrNull(index + 1)?.room)
                }

                scheduleUpcoming(lesson, day.getOrNull(index - 1), intent, studentId)
                scheduleCurrent(lesson, intent, studentId)
                if (numberOfLessons - 1 == index) scheduleLastLessonCancellation(lesson, intent, studentId)
            }
        }

        Timber.d("Timetable notifications scheduled")
    }

    private fun scheduleUpcoming(lesson: Timetable, previous: Timetable?, intent: Intent, studentId: Int) {
        if (lesson.start >= now()) {
            Timber.d("${lesson.start} >= ${now()}")
            val upcomingTimestamp = previous?.end?.toTimestamp() ?: lesson.start.minusMinutes(30).toTimestamp()
            AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, upcomingTimestamp,
                PendingIntent.getBroadcast(context, (upcomingTimestamp * studentId).toInt(), intent.also { it.putExtra(LESSON_TYPE, NOTIFICATION_TYPE_UPCOMING) }, PendingIntent.FLAG_CANCEL_CURRENT)
            )
            Timber.d("- scheduleUpcoming(): scheduled at ${upcomingTimestamp.toLocalDateTime()} (${previous?.end ?: lesson.start.minusMinutes(30)}) $lesson")
        } else Timber.d("- scheduleUpcoming(): not scheduled")
    }

    private fun scheduleCurrent(lesson: Timetable, intent: Intent, studentId: Int) {
        if (lesson.end > now()) {
            Timber.d("${lesson.end} > ${now()}")
            val currentTimestamp = lesson.start.toTimestamp()
            AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, currentTimestamp,
                PendingIntent.getBroadcast(context, (currentTimestamp * studentId).toInt(), intent.also { it.putExtra(LESSON_TYPE, NOTIFICATION_TYPE_CURRENT) }, PendingIntent.FLAG_CANCEL_CURRENT)
            )
            Timber.d("- scheduleCurrent(): scheduled at ${currentTimestamp.toLocalDateTime()} (${lesson.start}) $lesson")
        } else Timber.d("- scheduleCurrent(): not scheduled")
    }

    private fun scheduleLastLessonCancellation(lesson: Timetable, intent: Intent, studentId: Int) {
        if (lesson.end > now()) {
            Timber.d("${lesson.end} > ${now()}")
            val endTimestamp = lesson.end.toTimestamp()
            AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, endTimestamp,
                PendingIntent.getBroadcast(context, (endTimestamp * studentId).toInt(), intent.also { it.putExtra(LESSON_TYPE, NOTIFICATION_TYPE_LAST_LESSON_CANCELLATION) }, PendingIntent.FLAG_CANCEL_CURRENT)
            )
            Timber.d("- scheduleLastLessonCancellation(): scheduled at ${endTimestamp.toLocalDateTime()} (${lesson.end}) $lesson")
        } else Timber.d("- scheduleLastLessonCancellation(): not scheduled")
    }
}
