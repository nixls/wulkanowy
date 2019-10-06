package io.github.wulkanowy.ui.modules.timetablewidget

import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import io.github.wulkanowy.data.db.SharedPrefProvider
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.repositories.student.StudentRepository
import io.github.wulkanowy.ui.base.BasePresenter
import io.github.wulkanowy.ui.base.ErrorHandler
import io.github.wulkanowy.ui.modules.timetablewidget.TimetableWidgetProvider.Companion.getStudentWidgetKey
import io.github.wulkanowy.ui.modules.timetablewidget.TimetableWidgetProvider.Companion.getThemeWidgetKey
import io.github.wulkanowy.utils.SchedulersProvider
import javax.inject.Inject

class TimetableWidgetConfigurePresenter @Inject constructor(
    schedulers: SchedulersProvider,
    errorHandler: ErrorHandler,
    studentRepository: StudentRepository,
    private val sharedPref: SharedPrefProvider
) : BasePresenter<TimetableWidgetConfigureView>(errorHandler, studentRepository, schedulers) {

    private var appWidgetId: Int? = null

    private var isFromProvider = false

    private var selectedStudent: Student? = null

    fun onAttachView(view: TimetableWidgetConfigureView, appWidgetId: Int?, isFromProvider: Boolean?) {
        super.onAttachView(view)
        this.appWidgetId = appWidgetId
        this.isFromProvider = isFromProvider ?: false
        view.initView()
        loadData()
    }

    fun onItemSelect(item: AbstractFlexibleItem<*>) {
        if (item is TimetableWidgetConfigureItem) {
            selectedStudent = item.student

            if (isFromProvider) registerStudent(selectedStudent)
            else view?.showThemeDialog()
        }
    }

    fun onThemeSelect(index: Int) {
        appWidgetId?.let {
            sharedPref.putLong(getThemeWidgetKey(it), index.toLong())
        }
        registerStudent(selectedStudent)
    }

    fun onDismissThemeView(){
        view?.finishView()
    }

    private fun loadData() {
        disposable.add(studentRepository.getSavedStudents(false)
            .map { it to appWidgetId?.let { id -> sharedPref.getLong(getStudentWidgetKey(id), 0) } }
            .map { (students, currentStudentId) ->
                students.map { student -> TimetableWidgetConfigureItem(student, student.id == currentStudentId) }
            }
            .subscribeOn(schedulers.backgroundThread)
            .observeOn(schedulers.mainThread)
            .subscribe({
                when {
                    it.isEmpty() -> view?.openLoginView()
                    it.size == 1 && !isFromProvider -> {
                        selectedStudent = it.single().student
                        view?.showThemeDialog()
                    }
                    else -> view?.updateData(it)
                }
            }, { errorHandler.dispatch(it) }))
    }

    private fun registerStudent(student: Student?) {
        requireNotNull(student)

        appWidgetId?.let { id ->
            sharedPref.putLong(getStudentWidgetKey(id), student.id)
            view?.run {
                updateTimetableWidget(id)
                setSuccessResult(id)
            }
        }
        view?.finishView()
    }
}
