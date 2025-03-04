package io.github.wulkanowy.ui.modules.login.advanced

import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.repositories.student.StudentRepository
import io.github.wulkanowy.sdk.Sdk
import io.github.wulkanowy.ui.base.BasePresenter
import io.github.wulkanowy.ui.modules.login.LoginErrorHandler
import io.github.wulkanowy.utils.FirebaseAnalyticsHelper
import io.github.wulkanowy.utils.SchedulersProvider
import io.github.wulkanowy.utils.ifNullOrBlank
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject

class LoginAdvancedPresenter @Inject constructor(
    schedulers: SchedulersProvider,
    studentRepository: StudentRepository,
    private val loginErrorHandler: LoginErrorHandler,
    private val analytics: FirebaseAnalyticsHelper
) : BasePresenter<LoginAdvancedView>(loginErrorHandler, studentRepository, schedulers) {

    override fun onAttachView(view: LoginAdvancedView) {
        super.onAttachView(view)
        view.run {
            initView()
            showOnlyScrapperModeInputs()
            loginErrorHandler.onBadCredentials = {
                setErrorPassIncorrect()
                showSoftKeyboard()
                Timber.i("Entered wrong username or password")
            }
        }
    }

    fun onHostSelected() {
        view?.apply {
            clearPassError()
            clearNameError()
            if (formHostValue?.contains("fakelog") == true) {
                setDefaultCredentials("jan@fakelog.cf", "jan123", "powiatwulkanowy", "FK100000", "999999")
            }
        }
    }

    fun onLoginModeSelected(type: Sdk.Mode) {
        view?.run {
            when (type) {
                Sdk.Mode.API -> showOnlyMobileApiModeInputs()
                Sdk.Mode.SCRAPPER -> showOnlyScrapperModeInputs()
                Sdk.Mode.HYBRID -> showOnlyHybridModeInputs()
            }
        }
    }

    fun onPassTextChanged() {
        view?.clearPassError()
    }

    fun onNameTextChanged() {
        view?.clearNameError()
    }

    fun onPinTextChanged() {
        view?.clearPinKeyError()
    }

    fun onSymbolTextChanged() {
        view?.clearSymbolError()
    }

    fun onTokenTextChanged() {
        view?.clearTokenError()
    }

    fun onSignInClick() {
        if (!validateCredentials()) return

        disposable.add(getStudentsAppropriatesToLoginType()
            .subscribeOn(schedulers.backgroundThread)
            .observeOn(schedulers.mainThread)
            .doOnSubscribe {
                view?.apply {
                    hideSoftKeyboard()
                    showProgress(true)
                    showContent(false)
                }
                Timber.i("Login started")
            }
            .doFinally {
                view?.apply {
                    showProgress(false)
                    showContent(true)
                }
            }
            .subscribe({
                Timber.i("Login result: Success")
                analytics.logEvent("registration_form", "success" to true, "students" to it.size, "error" to "No error")
                view?.notifyParentAccountLogged(it)
            }, {
                Timber.i("Login result: An exception occurred")
                analytics.logEvent("registration_form", "success" to false, "students" to -1, "error" to it.message.ifNullOrBlank { "No message" })
                loginErrorHandler.dispatch(it)
            }))
    }

    private fun getStudentsAppropriatesToLoginType(): Single<List<Student>> {
        val email = view?.formNameValue.orEmpty()
        val password = view?.formPassValue.orEmpty()
        val endpoint = view?.formHostValue.orEmpty()

        val pin = view?.formPinValue.orEmpty()
        val symbol = view?.formSymbolValue.orEmpty()
        val token = view?.formTokenValue.orEmpty()

        return when (Sdk.Mode.valueOf(view?.formLoginType ?: "")) {
            Sdk.Mode.API -> studentRepository.getStudentsApi(pin, symbol, token)
            Sdk.Mode.SCRAPPER -> studentRepository.getStudentsScrapper(email, password, endpoint, symbol)
            Sdk.Mode.HYBRID -> studentRepository.getStudentsHybrid(email, password, endpoint, symbol)
        }
    }

    private fun validateCredentials(): Boolean {
        val login = view?.formNameValue.orEmpty()
        val password = view?.formPassValue.orEmpty()

        val pin = view?.formPinValue.orEmpty()
        val symbol = view?.formSymbolValue.orEmpty()
        val token = view?.formTokenValue.orEmpty()

        var isCorrect = true

        when (Sdk.Mode.valueOf(view?.formLoginType ?: "")) {
            Sdk.Mode.API -> {
                if (pin.isEmpty()) {
                    view?.setErrorPinRequired()
                    isCorrect = false
                }

                if (symbol.isEmpty()) {
                    view?.setErrorSymbolRequired()
                    isCorrect = false
                }

                if (token.isEmpty()) {
                    view?.setErrorTokenRequired()
                    isCorrect = false
                }
            }
            Sdk.Mode.SCRAPPER -> {
                if (login.isEmpty()) {
                    view?.setErrorNameRequired()
                    isCorrect = false
                }

                if (password.isEmpty()) {
                    view?.setErrorPassRequired(focus = isCorrect)
                    isCorrect = false
                }

                if (password.length < 6 && password.isNotEmpty()) {
                    view?.setErrorPassInvalid(focus = isCorrect)
                    isCorrect = false
                }
            }
            Sdk.Mode.HYBRID -> {
                if (login.isEmpty()) {
                    view?.setErrorNameRequired()
                    isCorrect = false
                }

                if (password.isEmpty()) {
                    view?.setErrorPassRequired(focus = isCorrect)
                    isCorrect = false
                }

                if (password.length < 6 && password.isNotEmpty()) {
                    view?.setErrorPassInvalid(focus = isCorrect)
                    isCorrect = false
                }
            }
        }

        return isCorrect
    }
}
