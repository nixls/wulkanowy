package io.github.wulkanowy.data.repositories.attendance

import io.github.wulkanowy.api.Api
import io.github.wulkanowy.api.attendance.Absent
import io.github.wulkanowy.data.db.entities.Attendance
import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.utils.toLocalDate
import io.reactivex.Single
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRemote @Inject constructor(private val api: Api) {

    fun getAttendance(semester: Semester, startDate: LocalDate, endDate: LocalDate): Single<List<Attendance>> {
        return Single.just(api.apply { diaryId = semester.diaryId })
            .flatMap { it.getAttendance(startDate, endDate) }.map { attendance ->
                attendance.map {
                    Attendance(
                        studentId = semester.studentId,
                        diaryId = semester.diaryId,
                        timeId = it.timeId,
                        date = it.date.toLocalDate(),
                        number = it.number,
                        subject = it.subject,
                        name = it.name,
                        presence = it.presence,
                        absence = it.absence,
                        exemption = it.exemption,
                        lateness = it.lateness,
                        excused = it.excused,
                        deleted = it.deleted,
                        excusable = it.excusable,
                        excuseStatus = it.excuseStatus?.name
                    )
                }
            }
    }

    fun excuseAbsence(semester: Semester, absenceList: List<Attendance>, reason: String?): Single<Boolean> {
        return Single.just(api.apply { diaryId = semester.diaryId })
            .flatMap {
                it.excuseForAbsence(absenceList.map { attendance ->
                    Absent(
                        date = LocalDateTime.of(attendance.date, LocalTime.of(0, 0)),
                        timeId = attendance.timeId
                    )
                }, reason)
            }
    }
}
