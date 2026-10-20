package com.example.swpp.tododapp.tasks

import java.time.LocalDate
import java.time.Period

/**
 * Builds the D-day label shown next to a task in the list.
 *
 * - Completed task: "CLEAR", whether or not it has a due date
 * - Active task with no due date: "" (nothing is shown)
 * - Due today: "D-Day"
 * - Due in N days: "D-N"
 * - Overdue by N days: "D+N"
 *
 * N is the number of calendar days between [today] and [dueDate], so it stays correct
 * across month ends, year ends and leap days.
 */
internal fun dueDateLabel(dueDate: LocalDate?, today: LocalDate, isCompleted: Boolean): String {
    if (isCompleted) return "CLEAR"
    if (dueDate == null) return ""
    val daysLeft = Period.between(today, dueDate).days.toLong()
    return when {
        daysLeft == 0L -> "D-Day"
        daysLeft > 0 -> "D-$daysLeft"
        else -> "D+${-daysLeft}"
    }
}
