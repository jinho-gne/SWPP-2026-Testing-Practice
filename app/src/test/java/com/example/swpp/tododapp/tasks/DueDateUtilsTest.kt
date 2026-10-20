package com.example.swpp.tododapp.tasks

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [dueDateLabel], one per Tip in specs/DueDateUtils.spec.md.
 * Every test compares the whole label string, so the format rule is checked everywhere.
 */
class DueDateUtilsTest {

    private fun date(text: String): LocalDate = LocalDate.parse(text)

    /** Given [due], [today] and [completed] / When dueDateLabel is called / Then it returns [expected]. */
    private fun assertLabel(expected: String, due: String?, today: String, completed: Boolean) {
        // GIVEN
        val dueDate = due?.let(::date)
        val todayDate = date(today)
        // WHEN
        val result = dueDateLabel(dueDate, todayDate, completed)
        // THEN
        assertEquals(expected, result)
    }

    // --- Success cases ---------------------------------------------------------------------

    @Test
    fun dueDateLabel_completedWithFutureDueDate_returnsClear() =
        assertLabel("CLEAR", due = "2026-05-25", today = "2026-05-20", completed = true)

    @Test
    fun dueDateLabel_completedWithNoDueDate_returnsClear() =
        assertLabel("CLEAR", due = null, today = "2026-05-20", completed = true)

    @Test
    fun dueDateLabel_activeWithNoDueDate_returnsEmpty() =
        assertLabel("", due = null, today = "2026-05-20", completed = false)

    @Test
    fun dueDateLabel_activeDueToday_returnsDDay() =
        assertLabel("D-Day", due = "2026-05-20", today = "2026-05-20", completed = false)

    @Test
    fun dueDateLabel_activeDueInFiveDays_returnsDMinus5() =
        assertLabel("D-5", due = "2026-05-25", today = "2026-05-20", completed = false)

    @Test
    fun dueDateLabel_activeOverdueByFiveDays_returnsDPlus5() =
        assertLabel("D+5", due = "2026-05-15", today = "2026-05-20", completed = false)

    @Test
    fun dueDateLabel_activeDueIn100Days_returnsDMinus100() =
        assertLabel("D-100", due = "2026-08-28", today = "2026-05-20", completed = false)

    @Test
    fun dueDateLabel_activeOverdueBy100Days_returnsDPlus100() =
        assertLabel("D+100", due = "2026-02-09", today = "2026-05-20", completed = false)

    // --- Edge cases: completion wins -------------------------------------------------------

    @Test
    fun dueDateLabel_completedDueToday_returnsClear() =
        assertLabel("CLEAR", due = "2026-05-20", today = "2026-05-20", completed = true)

    @Test
    fun dueDateLabel_completedOverdueByOneDay_returnsClear() =
        assertLabel("CLEAR", due = "2026-05-19", today = "2026-05-20", completed = true)

    @Test
    fun dueDateLabel_completedDueInOneMonth_returnsClear() =
        assertLabel("CLEAR", due = "2026-04-01", today = "2026-03-01", completed = true)

    // --- Edge cases: D-Day boundary --------------------------------------------------------

    @Test
    fun dueDateLabel_activeDueTomorrow_returnsDMinus1() =
        assertLabel("D-1", due = "2026-05-21", today = "2026-05-20", completed = false)

    @Test
    fun dueDateLabel_activeDueYesterday_returnsDPlus1() =
        assertLabel("D+1", due = "2026-05-19", today = "2026-05-20", completed = false)

    // --- Edge cases: month end -------------------------------------------------------------

    @Test
    fun dueDateLabel_dueOnLastDayOfMonth_returnsDMinus1() =
        assertLabel("D-1", due = "2026-01-31", today = "2026-01-30", completed = false)

    @Test
    fun dueDateLabel_dueOnFirstDayOfNextMonth_returnsDMinus1() =
        assertLabel("D-1", due = "2026-02-01", today = "2026-01-31", completed = false)

    @Test
    fun dueDateLabel_dueTwoDaysAcrossMonthEnd_returnsDMinus2() =
        assertLabel("D-2", due = "2026-02-01", today = "2026-01-30", completed = false)

    // --- Edge cases: one-month mark --------------------------------------------------------

    @Test
    fun dueDateLabel_dueOneDayBeforeOneMonthMark_returnsDMinus30() =
        assertLabel("D-30", due = "2026-03-31", today = "2026-03-01", completed = false)

    @Test
    fun dueDateLabel_dueAtOneMonthMark_returnsDMinus31() =
        assertLabel("D-31", due = "2026-04-01", today = "2026-03-01", completed = false)

    @Test
    fun dueDateLabel_dueOneDayAfterOneMonthMark_returnsDMinus32() =
        assertLabel("D-32", due = "2026-04-02", today = "2026-03-01", completed = false)

    @Test
    fun dueDateLabel_overdueOneDayBeforeOneMonthMark_returnsDPlus30() =
        assertLabel("D+30", due = "2026-03-02", today = "2026-04-01", completed = false)

    @Test
    fun dueDateLabel_overdueAtOneMonthMark_returnsDPlus31() =
        assertLabel("D+31", due = "2026-03-01", today = "2026-04-01", completed = false)

    @Test
    fun dueDateLabel_overdueOneDayAfterOneMonthMark_returnsDPlus32() =
        assertLabel("D+32", due = "2026-02-28", today = "2026-04-01", completed = false)

    // --- Edge cases: crossing a 28-day February --------------------------------------------

    @Test
    fun dueDateLabel_due28DaysAheadAtEndOfFebruary_returnsDMinus28() =
        assertLabel("D-28", due = "2026-02-28", today = "2026-01-31", completed = false)

    @Test
    fun dueDateLabel_due29DaysAheadIntoMarch_returnsDMinus29() =
        assertLabel("D-29", due = "2026-03-01", today = "2026-01-31", completed = false)

    @Test
    fun dueDateLabel_due30DaysAheadAcrossFebruary_returnsDMinus30() =
        assertLabel("D-30", due = "2026-03-02", today = "2026-01-31", completed = false)

    // --- Edge cases: whole calendar months -------------------------------------------------

    @Test
    fun dueDateLabel_dueExactlyOneCalendarMonthAhead_returnsDMinus31() =
        assertLabel("D-31", due = "2026-02-15", today = "2026-01-15", completed = false)

    @Test
    fun dueDateLabel_overdueExactlyOneCalendarMonth_returnsDPlus31() =
        assertLabel("D+31", due = "2026-01-15", today = "2026-02-15", completed = false)

    @Test
    fun dueDateLabel_overdue30DaysAcrossMonthEnd_returnsDPlus30() =
        assertLabel("D+30", due = "2026-01-31", today = "2026-03-02", completed = false)

    // --- Edge cases: year end --------------------------------------------------------------

    @Test
    fun dueDateLabel_dueNewYearsDayFromNewYearsEve_returnsDMinus1() =
        assertLabel("D-1", due = "2027-01-01", today = "2026-12-31", completed = false)

    @Test
    fun dueDateLabel_dueNewYearsDayFromDecember30_returnsDMinus2() =
        assertLabel("D-2", due = "2027-01-01", today = "2026-12-30", completed = false)

    @Test
    fun dueDateLabel_dueNewYearsEveSeenOnNewYearsDay_returnsDPlus1() =
        assertLabel("D+1", due = "2026-12-31", today = "2027-01-01", completed = false)

    @Test
    fun dueDateLabel_dueOneNonLeapYearAhead_returnsDMinus365() =
        assertLabel("D-365", due = "2027-01-01", today = "2026-01-01", completed = false)

    @Test
    fun dueDateLabel_dueOneLeapYearAhead_returnsDMinus366() =
        assertLabel("D-366", due = "2029-01-01", today = "2028-01-01", completed = false)

    // --- Edge cases: leap day --------------------------------------------------------------

    @Test
    fun dueDateLabel_dueLeapDayFromFebruary28_returnsDMinus1() =
        assertLabel("D-1", due = "2028-02-29", today = "2028-02-28", completed = false)

    @Test
    fun dueDateLabel_dueMarch1AcrossLeapDay_returnsDMinus2() =
        assertLabel("D-2", due = "2028-03-01", today = "2028-02-28", completed = false)

    @Test
    fun dueDateLabel_dueMarch1InNonLeapYear_returnsDMinus1() =
        assertLabel("D-1", due = "2027-03-01", today = "2027-02-28", completed = false)

    @Test
    fun dueDateLabel_dueLeapDaySeenOnMarch1_returnsDPlus1() =
        assertLabel("D+1", due = "2028-02-29", today = "2028-03-01", completed = false)

    @Test
    fun dueDateLabel_dueFebruary28SeenOnMarch1OfLeapYear_returnsDPlus2() =
        assertLabel("D+2", due = "2028-02-28", today = "2028-03-01", completed = false)
}
