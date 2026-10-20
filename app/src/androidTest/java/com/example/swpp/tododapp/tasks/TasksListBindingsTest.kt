package com.example.swpp.tododapp.tasks

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.example.swpp.tododapp.R
import com.example.swpp.tododapp.ServiceLocator
import com.example.swpp.tododapp.data.Task
import com.example.swpp.tododapp.data.source.FakeAndroidTestRepository
import com.example.swpp.tododapp.data.source.TasksRepository
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Integration tests for [setDueDateLabel], one per Tip in specs/TasksListBindings.spec.md.
 *
 * "shows label X"  = the view is VISIBLE and its text is exactly X.
 * "shows no label" = the text is "" or the view is not VISIBLE.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class TasksListBindingsTest {

    private lateinit var repository: TasksRepository
    private lateinit var today: LocalDate

    @Before
    fun setUp() {
        repository = FakeAndroidTestRepository()
        ServiceLocator.tasksRepository = repository
        today = LocalDate.now()
    }

    @After
    fun cleanupDb() = runTest {
        ServiceLocator.resetRepository()
    }

    // --- helpers: direct calls on a TextView -----------------------------------------------

    private fun onMain(block: () -> Unit) =
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    private fun newTextView(visibility: Int = View.VISIBLE): TextView {
        lateinit var view: TextView
        onMain {
            view = TextView(ApplicationProvider.getApplicationContext<Context>())
            view.visibility = visibility
        }
        return view
    }

    private fun bind(textView: TextView, task: Task?) = onMain { setDueDateLabel(textView, task) }

    private fun assertShowsLabel(expected: String, textView: TextView) {
        assertEquals("visibility", View.VISIBLE, textView.visibility)
        assertEquals(expected, textView.text.toString())
    }

    private fun assertShowsNoLabel(textView: TextView) {
        assertTrue(
            "expected no label but saw \"${textView.text}\" (visibility=${textView.visibility})",
            textView.text.toString().isEmpty() || textView.visibility != View.VISIBLE
        )
    }

    // --- helpers: the task list screen -----------------------------------------------------

    private suspend fun launchListWith(task: Task) {
        repository.saveTask(task)
        launchFragmentInContainer<TasksFragment>(Bundle(), R.style.AppTheme)
    }

    private fun inRow(viewId: Int, title: String = "TITLE1"): Matcher<View> =
        allOf(withId(viewId), hasSibling(withText(title)))

    private val showsNoLabel: Matcher<View> =
        anyOf(withText(""), not(withEffectiveVisibility(Visibility.VISIBLE)))

    private fun showsLabel(expected: String): Matcher<View> =
        allOf(withEffectiveVisibility(Visibility.VISIBLE), withText(expected))

    /** The list diff runs off the main thread, so poll briefly before failing. */
    private fun checkRowLabel(matcher: Matcher<View>, timeoutMs: Long = 3000) {
        val end = System.currentTimeMillis() + timeoutMs
        while (true) {
            try {
                onView(inRow(R.id.due_date_text)).check(matches(matcher))
                return
            } catch (e: AssertionError) {
                if (System.currentTimeMillis() > end) throw e
                Thread.sleep(100)
            }
        }
    }

    private fun clickRowCheckbox() = onView(inRow(R.id.complete_checkbox)).perform(click())

    // --- Success cases ---------------------------------------------------------------------

    @Test
    fun setDueDateLabel_completedTaskWithDueDate_showsClear() {
        // GIVEN
        val textView = newTextView(View.GONE)
        val task = Task("Title", "Description", isCompleted = true, dueDate = today.plusDays(3))
        // WHEN
        bind(textView, task)
        // THEN
        assertShowsLabel("CLEAR", textView)
    }

    @Test
    fun setDueDateLabel_activeTaskDueInThreeDays_showsDMinus3() {
        // GIVEN - an active task due in 3 days
        val textView = newTextView(View.GONE)
        val task = Task("Title", "Description", isCompleted = false, dueDate = today.plusDays(3))
        // WHEN
        bind(textView, task)
        // THEN
        assertShowsLabel("D-3", textView)
    }

    @Test
    fun setDueDateLabel_activeTaskOverdueByThreeDays_showsDPlus3() {
        // GIVEN - an active task overdue by 3 days
        val textView = newTextView(View.GONE)
        val task = Task("Title", "Description", isCompleted = false, dueDate = today.minusDays(3))
        // WHEN
        bind(textView, task)
        // THEN
        assertShowsLabel("D+3", textView)
    }

    @Test
    fun setDueDateLabel_activeTaskWithNoDueDate_showsNoLabel() {
        // GIVEN - an active task without a due date
        val textView = newTextView()
        val task = Task("Title", "Description", isCompleted = false, dueDate = null)
        // WHEN
        bind(textView, task)
        // THEN
        assertShowsNoLabel(textView)
    }

    @Test
    fun setDueDateLabel_nullTask_showsNoLabel() {
        // GIVEN - a new TextView
        val textView = newTextView()
        // WHEN - it is bound to no task
        bind(textView, null)
        // THEN
        assertShowsNoLabel(textView)
    }

    @Test
    fun setDueDateLabel_checkActiveTaskWithNoDueDateInList_showsClear() = runTest {
        // GIVEN - one active task without a due date on the list screen
        launchListWith(Task("TITLE1", "DESCRIPTION1", false, "id1", dueDate = null))
        // WHEN - its checkbox is checked
        clickRowCheckbox()
        // THEN - its row shows CLEAR
        checkRowLabel(showsLabel("CLEAR"))
    }

    @Test
    fun setDueDateLabel_checkActiveTaskWithDueDateInList_replacesDDayWithClear() = runTest {
        // GIVEN - one active task due in 2 days on the list screen, showing D-2
        launchListWith(Task("TITLE1", "DESCRIPTION1", false, "id1", dueDate = today.plusDays(2)))
        checkRowLabel(showsLabel("D-2"))
        // WHEN - its checkbox is checked
        clickRowCheckbox()
        // THEN - D-2 is replaced by CLEAR
        checkRowLabel(showsLabel("CLEAR"))
    }

    // --- Edge cases: D-Day boundary --------------------------------------------------------

    @Test
    fun setDueDateLabel_activeTaskDueToday_showsDDay() {
        // GIVEN - an active task due today
        val textView = newTextView(View.GONE)
        val task = Task("Title", "Description", isCompleted = false, dueDate = today)
        // WHEN
        bind(textView, task)
        // THEN
        assertShowsLabel("D-Day", textView)
    }

    @Test
    fun setDueDateLabel_activeTaskDueTomorrow_showsDMinus1() {
        // GIVEN - an active task due tomorrow
        val textView = newTextView(View.GONE)
        val task = Task("Title", "Description", isCompleted = false, dueDate = today.plusDays(1))
        // WHEN
        bind(textView, task)
        // THEN
        assertShowsLabel("D-1", textView)
    }

    @Test
    fun setDueDateLabel_activeTaskDueYesterday_showsDPlus1() {
        // GIVEN - an active task due yesterday
        val textView = newTextView(View.GONE)
        val task = Task("Title", "Description", isCompleted = false, dueDate = today.minusDays(1))
        // WHEN
        bind(textView, task)
        // THEN
        assertShowsLabel("D+1", textView)
    }

    @Test
    fun setDueDateLabel_completedTaskWithNoDueDate_showsClear() {
        // GIVEN - a completed task without a due date
        val textView = newTextView(View.GONE)
        val task = Task("Title", "Description", isCompleted = true, dueDate = null)
        // WHEN
        bind(textView, task)
        // THEN
        assertShowsLabel("CLEAR", textView)
    }

    // --- Edge cases: recycled views --------------------------------------------------------

    @Test
    fun setDueDateLabel_recycledFromDDayLabelToNoDueDate_showsNoLabel() {
        // GIVEN - the view shows D-1 for an earlier task
        val textView = newTextView()
        bind(textView, Task("First", "Description", isCompleted = false, dueDate = today.plusDays(1)))
        assertShowsLabel("D-1", textView)
        // WHEN - it is rebound to an active task with no due date
        bind(textView, Task("Second", "Description", isCompleted = false, dueDate = null))
        // THEN
        assertShowsNoLabel(textView)
    }

    @Test
    fun setDueDateLabel_recycledFromLabelToNullTask_showsNoLabel() {
        // GIVEN - the view shows D-Day for an earlier task
        val textView = newTextView()
        bind(textView, Task("First", "Description", isCompleted = false, dueDate = today))
        assertShowsLabel("D-Day", textView)
        // WHEN - it is rebound to no task
        bind(textView, null)
        // THEN
        assertShowsNoLabel(textView)
    }

    @Test
    fun setDueDateLabel_recycledFromNoLabelToOverdueTask_showsDPlus1() {
        // GIVEN - the view shows no label for an active task without a due date
        val textView = newTextView()
        bind(textView, Task("First", "Description", isCompleted = false, dueDate = null))
        assertShowsNoLabel(textView)
        // WHEN - it is rebound to a task overdue by 1 day
        bind(textView, Task("Second", "Description", isCompleted = false, dueDate = today.minusDays(1)))
        // THEN
        assertShowsLabel("D+1", textView)
    }

    @Test
    fun setDueDateLabel_recycledSameTaskCheckedOff_showsClear() {
        // GIVEN - the view shows D-1 for an active task
        val textView = newTextView()
        val due = today.plusDays(1)
        bind(textView, Task("Title", "Description", isCompleted = false, id = "task1", dueDate = due))
        assertShowsLabel("D-1", textView)
        // WHEN - it is rebound to the same task, now completed
        bind(textView, Task("Title", "Description", isCompleted = true, id = "task1", dueDate = due))
        // THEN
        assertShowsLabel("CLEAR", textView)
    }

    @Test
    fun setDueDateLabel_recycledSameTaskWithDueDateUnchecked_showsDMinus1() {
        // GIVEN - the view shows CLEAR for a completed task due tomorrow
        val textView = newTextView()
        val due = today.plusDays(1)
        bind(textView, Task("Title", "Description", isCompleted = true, id = "task1", dueDate = due))
        assertShowsLabel("CLEAR", textView)
        // WHEN - it is rebound to the same task, now active again
        bind(textView, Task("Title", "Description", isCompleted = false, id = "task1", dueDate = due))
        // THEN
        assertShowsLabel("D-1", textView)
    }

    @Test
    fun setDueDateLabel_recycledSameTaskWithNoDueDateUnchecked_showsNoLabel() {
        // GIVEN - the view shows CLEAR for a completed task without a due date
        val textView = newTextView()
        bind(textView, Task("Title", "Description", isCompleted = true, id = "task1", dueDate = null))
        assertShowsLabel("CLEAR", textView)
        // WHEN - it is rebound to the same task, now active again
        bind(textView, Task("Title", "Description", isCompleted = false, id = "task1", dueDate = null))
        // THEN - CLEAR does not stay
        assertShowsNoLabel(textView)
    }

    // --- Edge cases: check and uncheck on the list screen ----------------------------------

    @Test
    fun setDueDateLabel_checkThenUncheckTaskWithDueDateInList_showsDMinus2() = runTest {
        // GIVEN - one active task due in 2 days on the list screen
        launchListWith(Task("TITLE1", "DESCRIPTION1", false, "id1", dueDate = today.plusDays(2)))
        // WHEN - its checkbox is checked, then unchecked
        clickRowCheckbox()
        checkRowLabel(showsLabel("CLEAR"))
        clickRowCheckbox()
        // THEN - the D-day label is back
        checkRowLabel(showsLabel("D-2"))
    }

    @Test
    fun setDueDateLabel_checkThenUncheckTaskWithNoDueDateInList_showsNoLabel() = runTest {
        // GIVEN - one active task without a due date on the list screen
        launchListWith(Task("TITLE1", "DESCRIPTION1", false, "id1", dueDate = null))
        // WHEN - its checkbox is checked, then unchecked
        clickRowCheckbox()
        checkRowLabel(showsLabel("CLEAR"))
        clickRowCheckbox()
        // THEN - CLEAR does not stay
        checkRowLabel(showsNoLabel)
    }
}
