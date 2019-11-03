package io.github.wulkanowy.services.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.wulkanowy.R
import io.github.wulkanowy.services.sync.channels.UpcomingLessonsChannel
import io.github.wulkanowy.utils.getCompatColor
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import timber.log.Timber

class AlarmBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val LESSON_TITLE = "title"
        const val LESSON_ROOM = "room"
        const val LESSON_START = "start_timestamp"
        const val LESSON_END = "end_timestamp"
        const val LESSON_TYPE = "type"
        const val NOTIFICATION_TYPE_CURRENT = 1
        const val NOTIFICATION_TYPE_UPCOMING = 2
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val type = intent!!.getIntExtra(LESSON_TYPE, 0)
        val subject = intent.getStringExtra(LESSON_TITLE)
        val room = intent.getStringExtra(LESSON_ROOM)!!
        val start = intent.getLongExtra(LESSON_START, 0)
        val end = intent.getLongExtra(LESSON_END, 0)
        Timber.d("AlarmBroadcastReceiver receive intent: type: $type, subject: $subject, room: $room, start: $start")

//        NotificationManagerCompat.from(context).cancel(type)
        NotificationManagerCompat.from(context).cancelAll()
        showNotification(context, type, subject, room, start, end)
    }

    private fun showNotification(context: Context, type: Int, subject: String?, room: String, start: Long, end: Long) {
        NotificationManagerCompat.from(context).notify(type, NotificationCompat.Builder(context, UpcomingLessonsChannel.CHANNEL_ID)
            .setContentTitle((if (type == NOTIFICATION_TYPE_CURRENT) context.getString(R.string.timetable_now) else context.getString(R.string.timetable_next)) + ": $subject ($room)")
            .setContentText("od ${start.toLocalDateTime()} do ${end.toLocalDateTime()}")
            .setUsesChronometer(true)
            .setAutoCancel(false)
            .setOngoing(true)
            .setWhen(if (type == NOTIFICATION_TYPE_CURRENT) end else start)
            .setColor(context.getCompatColor(R.color.colorPrimary))
            .setSmallIcon(R.drawable.ic_main_timetable)
            .build()
        )
    }

    private fun Long.toLocalDateTime() = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
}
