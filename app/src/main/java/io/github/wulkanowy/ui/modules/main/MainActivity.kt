package io.github.wulkanowy.ui.modules.main

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.ViewCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.elevation.ElevationOverlayProvider
import com.ncapdevi.fragnav.FragNavController
import com.ncapdevi.fragnav.FragNavController.Companion.HIDE
import dagger.Lazy
import io.github.wulkanowy.R
import io.github.wulkanowy.ui.base.BaseActivity
import io.github.wulkanowy.ui.modules.about.AboutFragment
import io.github.wulkanowy.ui.modules.account.AccountDialog
import io.github.wulkanowy.ui.modules.attendance.AttendanceFragment
import io.github.wulkanowy.ui.modules.exam.ExamFragment
import io.github.wulkanowy.ui.modules.grade.GradeFragment
import io.github.wulkanowy.ui.modules.homework.HomeworkFragment
import io.github.wulkanowy.ui.modules.luckynumber.LuckyNumberFragment
import io.github.wulkanowy.ui.modules.message.MessageFragment
import io.github.wulkanowy.ui.modules.mobiledevice.MobileDeviceFragment
import io.github.wulkanowy.ui.modules.note.NoteFragment
import io.github.wulkanowy.ui.modules.settings.SettingsFragment
import io.github.wulkanowy.ui.modules.timetable.TimetableFragment
import io.github.wulkanowy.utils.dpToPx
import io.github.wulkanowy.utils.safelyPopFragments
import io.github.wulkanowy.utils.setOnViewChangeListener
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

class MainActivity : BaseActivity<MainPresenter>(), MainView,
    FragNavController.RootFragmentListener {

    @Inject
    override lateinit var presenter: MainPresenter

    @Inject
    lateinit var navController: FragNavController

    @Inject
    lateinit var overlayProvider: Lazy<ElevationOverlayProvider>

    companion object {
        const val EXTRA_START_MENU = "extraStartMenu"

        fun getStartIntent(context: Context, startMenu: MainView.Section? = null, clear: Boolean = false): Intent {
            return Intent(context, MainActivity::class.java)
                .apply {
                    if (clear) flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
                    startMenu?.let { putExtra(EXTRA_START_MENU, it) }
                }
        }
    }

    override val numberOfRootFragments get() = 11

    override val isRootView get() = navController.isRootFragment

    override val currentStackSize get() = navController.currentStack?.size

    override val currentViewTitle get() = (navController.currentFrag as? MainView.TitledView)?.titleStringId?.let { getString(it) }

    override var startMenuIndex = 0

    override var startMenuMoreIndex = -1

    private val moreMenuFragments = mapOf<Int, Fragment>(
        MainView.Section.MESSAGE.id to MessageFragment.newInstance(),
        MainView.Section.HOMEWORK.id to HomeworkFragment.newInstance(),
        MainView.Section.NOTE.id to NoteFragment.newInstance(),
        MainView.Section.LUCKY_NUMBER.id to LuckyNumberFragment.newInstance()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(mainToolbar)
        messageContainer = mainFragmentContainer

        presenter.onAttachView(this, intent.getSerializableExtra(EXTRA_START_MENU) as? MainView.Section)

        with(navController) {
            initialize(startMenuIndex, savedInstanceState)
            pushFragment(moreMenuFragments[startMenuMoreIndex])
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_menu_main, menu)
        return true
    }

    override fun initView() {
        with(mainToolbar) {
            if (SDK_INT >= LOLLIPOP) stateListAnimator = null
            setBackgroundColor(overlayProvider.get().getSurfaceColorWithOverlayIfNeeded(dpToPx(4f)))
        }

        val toggle = ActionBarDrawerToggle(this, mainDrawer, mainToolbar, android.R.string.ok, android.R.string.no)
        mainDrawer.addDrawerListener(toggle)
        toggle.syncState()

        mainNavigationView.setCheckedItem(R.id.drawerMenuGrade)

        mainNavigationView.setNavigationItemSelectedListener {
            presenter.onDrawerMenuSelected(when (it.itemId) {
                R.id.drawerMenuGrade -> 0
                R.id.drawerMenuAttendance -> 1
                R.id.drawerMenuExam -> 2
                R.id.drawerMenuTimetable -> 3
                R.id.drawerMenuMessage -> 4
                R.id.drawerMenuHomework -> 5
                R.id.drawerMenuNote -> 6
                R.id.drawerMenuLuckyNumber -> 7
                R.id.drawerMenuMobileDevices -> 8
                R.id.drawerMenuSettings -> 9
                R.id.drawerMenuAbout -> 10
                else -> 0
            })
        }

        with(navController) {
            setOnViewChangeListener(presenter::onViewChange)
            fragmentHideStrategy = HIDE
            rootFragmentListener = this@MainActivity
        }
    }

    override fun getRootFragment(index: Int): Fragment {
        return when (index) {
            0 -> GradeFragment.newInstance()
            1 -> AttendanceFragment.newInstance()
            2 -> ExamFragment.newInstance()
            3 -> TimetableFragment.newInstance()
            4 -> MessageFragment.newInstance()
            5 -> HomeworkFragment.newInstance()
            6 -> NoteFragment.newInstance()
            7 -> LuckyNumberFragment.newInstance()
            8 -> MobileDeviceFragment.newInstance()
            9 -> SettingsFragment.newInstance()
            10 -> AboutFragment.newInstance()
            else -> throw IllegalAccessException()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return if (item?.itemId == R.id.mainMenuAccount) presenter.onAccountManagerSelected()
        else false
    }

    override fun onSupportNavigateUp(): Boolean {
        return presenter.onUpNavigate()
    }

    override fun switchMenuView(position: Int) {
        navController.switchTab(position)
    }

    override fun setViewTitle(title: String) {
        supportActionBar?.title = title
    }

    override fun showHomeArrow(show: Boolean) {
        supportActionBar?.setDisplayHomeAsUpEnabled(show)
    }

    override fun showAccountPicker() {
        navController.showDialogFragment(AccountDialog.newInstance())
    }

    override fun showActionBarElevation(show: Boolean) {
        ViewCompat.setElevation(mainToolbar, if (show) dpToPx(4f) else 0f)
    }

    fun showDialogFragment(dialog: DialogFragment) {
        navController.showDialogFragment(dialog)
    }

    fun pushView(fragment: Fragment) {
        navController.pushFragment(fragment)
    }

    override fun popView(depth: Int) {
        navController.safelyPopFragments(depth)
    }

    override fun onBackPressed() {
        presenter.onBackPressed { super.onBackPressed() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        navController.onSaveInstanceState(outState)
        intent.removeExtra(EXTRA_START_MENU)
    }
}
