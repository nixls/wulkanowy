package io.github.wulkanowy.utils

import androidx.fragment.app.Fragment
import io.github.wulkanowy.ui.modules.about.AboutFragment
import io.github.wulkanowy.ui.modules.attendance.AttendanceFragment
import io.github.wulkanowy.ui.modules.exam.ExamFragment
import io.github.wulkanowy.ui.modules.grade.GradeFragment
import io.github.wulkanowy.ui.modules.homework.HomeworkFragment
import io.github.wulkanowy.ui.modules.luckynumber.LuckyNumberFragment
import io.github.wulkanowy.ui.modules.main.MainView
import io.github.wulkanowy.ui.modules.message.MessageFragment
import io.github.wulkanowy.ui.modules.note.NoteFragment
import io.github.wulkanowy.ui.modules.settings.SettingsFragment
import io.github.wulkanowy.ui.modules.timetable.TimetableFragment

fun Fragment.toSection(): MainView.Section? {
    return when (this) {
        is GradeFragment -> MainView.Section.GRADE
        is AttendanceFragment -> MainView.Section.ATTENDANCE
        is ExamFragment -> MainView.Section.EXAM
        is TimetableFragment -> MainView.Section.TIMETABLE
        is MessageFragment -> MainView.Section.MESSAGE
        is HomeworkFragment -> MainView.Section.HOMEWORK
        is NoteFragment -> MainView.Section.NOTE
        is LuckyNumberFragment -> MainView.Section.LUCKY_NUMBER
        is SettingsFragment -> MainView.Section.SETTINGS
        is AboutFragment -> MainView.Section.ABOUT
        else -> null
    }
}
