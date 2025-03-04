package io.github.wulkanowy.data.repositories.attendance

import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.sdk.Sdk
import io.github.wulkanowy.sdk.pojo.Attendance
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDate.of

class AttendanceRemoteTest {

    @MockK
    private lateinit var mockSdk: Sdk

    @MockK
    private lateinit var semesterMock: Semester

    @Before
    fun initApi() {
        MockKAnnotations.init(this)
    }

    @Test
    fun getAttendanceTest() {
        every {
            mockSdk.getAttendance(
                of(2018, 9, 10),
                of(2018, 9, 15),
                1
            )
        } returns Single.just(listOf(
            getAttendance(of(2018, 9, 10)),
            getAttendance(of(2018, 9, 17))
        ))

        every { semesterMock.studentId } returns 1
        every { semesterMock.diaryId } returns 1
        every { semesterMock.schoolYear } returns 2019
        every { semesterMock.semesterId } returns 1
        every { mockSdk.switchDiary(any(), any()) } returns mockSdk

        val attendance = AttendanceRemote(mockSdk).getAttendance(semesterMock,
            of(2018, 9, 10),
            of(2018, 9, 15)
        ).blockingGet()
        assertEquals(2, attendance.size)
    }

    private fun getAttendance(date: LocalDate): Attendance {
        return Attendance(
            subject = "Fizyka",
            name = "Obecność",
            date = date,
            number = 0,
            deleted = false,
            excusable = false,
            excused = false,
            exemption = false,
            lateness = false,
            presence = false,
            categoryId = 1,
            absence = false
        )
    }
}
