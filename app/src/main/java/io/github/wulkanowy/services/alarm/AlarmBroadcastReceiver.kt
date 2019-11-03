package io.github.wulkanowy.services.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.wulkanowy.R
import io.github.wulkanowy.services.sync.channels.UpcomingLessonsChannel
import io.github.wulkanowy.ui.modules.main.MainActivity
import io.github.wulkanowy.ui.modules.main.MainView
import io.github.wulkanowy.utils.getCompatColor
import timber.log.Timber

class AlarmBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val LESSON_TYPE = "type"
        const val NOTIFICATION_TYPE_CURRENT = 1
        const val NOTIFICATION_TYPE_UPCOMING = 2
        const val NOTIFICATION_TYPE_LAST_LESSON_CANCELLATION = 3

        const val LESSON_TITLE = "title"
        const val LESSON_ROOM = "room"

        const val LESSON_NEXT_TITLE = "next_title"
        const val LESSON_NEXT_ROOM = "next_room"

        const val LESSON_START = "start_timestamp"
        const val LESSON_END = "end_timestamp"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val type = intent!!.getIntExtra(LESSON_TYPE, 0)

        NotificationManagerCompat.from(context).cancelAll()
        if (type == NOTIFICATION_TYPE_LAST_LESSON_CANCELLATION) return

        val subject = intent.getStringExtra(LESSON_TITLE)
        val room = intent.getStringExtra(LESSON_ROOM)

        val nextSubject = intent.getStringExtra(LESSON_NEXT_TITLE)
        val nextRoom = intent.getStringExtra(LESSON_NEXT_ROOM)
        val start = intent.getLongExtra(LESSON_START, 0)
        val end = intent.getLongExtra(LESSON_END, 0)
        Timber.d("AlarmBroadcastReceiver receive intent: type: $type, subject: $subject, room: $room, start: $start")

        showNotification(context, type, context.getString(if (type == NOTIFICATION_TYPE_CURRENT) R.string.timetable_now else R.string.timetable_next, "$subject ($room)"),
            start, end, nextSubject?.let { context.getString(R.string.timetable_later, "$nextSubject ($nextRoom)") }
        )
    }

    private fun showNotification(context: Context, type: Int, title: String, start: Long, end: Long, next: String?) {
        NotificationManagerCompat.from(context).notify(type, NotificationCompat.Builder(context, UpcomingLessonsChannel.CHANNEL_ID)
            .setContentTitle(title)
            .setWhen(if (type == NOTIFICATION_TYPE_CURRENT) end else start)
            .setContentText(next)
            .setUsesChronometer(true)
            .setAutoCancel(false)
            .setOngoing(true)
            .setColor(context.getCompatColor(R.color.colorPrimary))
            .setSmallIcon(R.drawable.ic_main_timetable)
            .setContentIntent(PendingIntent.getActivity(context, MainView.Section.TIMETABLE.id,
                MainActivity.getStartIntent(context, MainView.Section.TIMETABLE, true), PendingIntent.FLAG_UPDATE_CURRENT))
            .build()
        )
    }
}
