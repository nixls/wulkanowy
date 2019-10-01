package io.github.wulkanowy.ui.modules.attendance

import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import io.github.wulkanowy.data.db.entities.Attendance
import io.github.wulkanowy.data.repositories.attendance.AttendanceRepository
import io.github.wulkanowy.data.repositories.preferences.PreferencesRepository
import io.github.wulkanowy.data.repositories.semester.SemesterRepository
import io.github.wulkanowy.data.repositories.student.StudentRepository
import io.github.wulkanowy.ui.base.BasePresenter
import io.github.wulkanowy.ui.base.ErrorHandler
import io.github.wulkanowy.utils.FirebaseAnalyticsHelper
import io.github.wulkanowy.utils.SchedulersProvider
import io.github.wulkanowy.utils.getLastSchoolDayIfHoliday
import io.github.wulkanowy.utils.isHolidays
import io.github.wulkanowy.utils.nextSchoolDay
import io.github.wulkanowy.utils.previousOrSameSchoolDay
import io.github.wulkanowy.utils.previousSchoolDay
import io.github.wulkanowy.utils.toFormattedString
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDate.now
import org.threeten.bp.LocalDate.ofEpochDay
import timber.log.Timber
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class AttendancePresenter @Inject constructor(
    schedulers: SchedulersProvider,
    errorHandler: ErrorHandler,
    studentRepository: StudentRepository,
    private val attendanceRepository: AttendanceRepository,
    private val semesterRepository: SemesterRepository,
    private val prefRepository: PreferencesRepository,
    private val analytics: FirebaseAnalyticsHelper
) : BasePresenter<AttendanceView>(errorHandler, studentRepository, schedulers) {

    private var baseDate: LocalDate = now().previousOrSameSchoolDay

    lateinit var currentDate: LocalDate
        private set

    private var attendanceToExcuseList: List<Attendance> = ArrayList()

    fun onAttachView(view: AttendanceView, date: Long?) {
        super.onAttachView(view)
        view.initView()
        Timber.i("Attendance view was initialized")
        loadData(ofEpochDay(date ?: baseDate.toEpochDay()))
        if (currentDate.isHolidays) setBaseDateOnHolidays()
        reloadView()
    }

    fun onPreviousDay() {
        loadData(currentDate.previousSchoolDay)
        reloadView()
    }

    fun onNextDay() {
        loadData(currentDate.nextSchoolDay)
        reloadView()
    }

    fun onSwipeRefresh() {
        Timber.i("Force refreshing the attendance")
        loadData(currentDate, true)
    }

    fun onViewReselected() {
        Timber.i("Attendance view is reselected")
        view?.also { view ->
            if (view.currentStackSize == 1) {
                baseDate.also {
                    if (currentDate != it) {
                        loadData(it)
                        reloadView()
                    } else if (!view.isViewEmpty) view.resetView()
                }
            } else view.popView()
        }
    }

    fun onAttendanceItemSelected(item: AbstractFlexibleItem<*>?) {
        if (item is AttendanceItem) {
            Timber.i("Select attendance item ${item.attendance.id}")
            view?.showAttendanceDialog(item.attendance)
        }
    }

    fun onExcuseCheckboxSelect(attendanceItem: Attendance, checked: Boolean) {
        attendanceToExcuseList = when (checked) {
            true -> attendanceToExcuseList + attendanceItem
            false -> attendanceToExcuseList - attendanceItem
        }

        view?.showExcuseButton(attendanceToExcuseList.isNotEmpty())
    }

    fun onExcuseButtonClick() {
        if (attendanceToExcuseList.isNotEmpty()) {
            view?.showExcuseDialog()
        }
    }

    fun onExcuseDialogSubmit(reason: String) {
        if (attendanceToExcuseList.isNotEmpty()) {
            excuseAbsence(if (reason != "") reason else null)
        }
    }

    fun onSummarySwitchSelected(): Boolean {
        view?.openSummaryView()
        return true
    }

    private fun setBaseDateOnHolidays() {
        disposable.add(studentRepository.getCurrentStudent()
            .flatMap { semesterRepository.getCurrentSemester(it) }
            .subscribeOn(schedulers.backgroundThread)
            .observeOn(schedulers.mainThread)
            .subscribe({
                baseDate = baseDate.getLastSchoolDayIfHoliday(it.schoolYear)
                currentDate = baseDate
                reloadNavigation()
            }) {
                Timber.i("Loading semester result: An exception occurred")
            })
    }

    private fun loadData(date: LocalDate, forceRefresh: Boolean = false) {
        Timber.i("Loading attendance data started")
        currentDate = date
        disposable.apply {
            clear()
            add(studentRepository.getCurrentStudent()
                .delay(200, MILLISECONDS)
                .flatMap { semesterRepository.getCurrentSemester(it) }
                .flatMap { attendanceRepository.getAttendance(it, date, date, forceRefresh) }
                .map { list ->
                    if (prefRepository.isShowPresent) list
                    else list.filter { !it.presence }
                }
                .map { items -> items.map { AttendanceItem(it) } }
                .map { items -> items.sortedBy { it.attendance.number } }
                .subscribeOn(schedulers.backgroundThread)
                .observeOn(schedulers.mainThread)
                .doFinally {
                    view?.run {
                        hideRefresh()
                        showProgress(false)
                        enableSwipe(true)
                    }
                }
                .subscribe({
                    Timber.i("Loading attendance result: Success")
                    view?.apply {
                        updateData(it)
                        showEmpty(it.isEmpty())
                        showContent(it.isNotEmpty())
                    }
                    analytics.logEvent("load_attendance", "items" to it.size, "force_refresh" to forceRefresh)
                }) {
                    Timber.i("Loading attendance result: An exception occurred")
                    view?.run { showEmpty(isViewEmpty) }
                    errorHandler.dispatch(it)
                }
            )
        }
    }

    private fun excuseAbsence(reason: String?) {
        Timber.i("Excusing absence started")
        disposable.apply {
            add(studentRepository.getCurrentStudent()
                .delay(200, MILLISECONDS)
                .flatMap { semesterRepository.getCurrentSemester(it) }
                .flatMap { attendanceRepository.excuseForAbsence(it, attendanceToExcuseList, reason) }
                .subscribeOn(schedulers.backgroundThread)
                .observeOn(schedulers.mainThread)
                .doOnSubscribe {
                    view?.showProgress(true)
                }
                .doFinally {
                    view?.showProgress(false)
                }
                .subscribe({
                    Timber.i("Excusing for absence result: Success")
                    analytics.logEvent("excuse_absence", "items" to attendanceToExcuseList.size)
                    attendanceToExcuseList = ArrayList()
                    view?.apply {
                        showExcuseButton(false)
                        showMessage(excuseSuccessString)
                    }
                    loadData(currentDate, true)
                }) {
                    Timber.i("Excusing for absence result: An exception occurred")
                    errorHandler.dispatch(it)
                })
        }
    }

    private fun reloadView() {
        Timber.i("Reload attendance view with the date ${currentDate.toFormattedString()}")
        view?.apply {
            showProgress(true)
            enableSwipe(false)
            showContent(false)
            showEmpty(false)
            clearData()
            reloadNavigation()
        }
    }

    private fun reloadNavigation() {
        view?.apply {
            showPreButton(!currentDate.minusDays(1).isHolidays)
            showNextButton(!currentDate.plusDays(1).isHolidays)
            updateNavigationDay(currentDate.toFormattedString("EEEE, dd.MM").capitalize())
        }
    }
}
