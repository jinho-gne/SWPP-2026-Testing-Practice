# Spec: `DueDateUtils.kt`

> Post-conditions are the test oracle: write the expected behavior from intent, not from the code.
> Tag each post-condition [Unit] if its test runs on the JVM; otherwise tag it [Integration].
> End each valid case with `Tested by <test>` if an existing test checks it, or `Tip:` describing the test to write.

## `internal fun dueDateLabel(dueDate: LocalDate?, today: LocalDate, isCompleted: Boolean): String`

**Intent**: Build the D-day label shown next to a task in the list. A completed task shows "CLEAR" whether or not it has a due date, an active task without a due date shows nothing, and an active task with a due date shows "D-N" when it is due in N days, "D-Day" when it is due today, and "D+N" when it is overdue by N days. N is the number of days you would count on a calendar between `today` and `dueDate` (2026-01-15 to 2026-02-15 is 31 days), so it stays correct across month ends, year ends and leap days. The result is exactly one of "CLEAR", "", "D-Day", "D-N" or "D+N", with N written as a plain decimal number (no leading zeros, padding or spaces), so every Tip below compares the whole string.

**Pre-conditions**
- `today` is the user's current local date. The caller passes it in, so the function never reads the clock.
- `dueDate` is either `null` (the task has no due date) or any valid `LocalDate`, in the past, today, or in the future.

**Post-conditions**
- Success cases
  - A completed task with a due date in the future returns "CLEAR". `[Unit]`
    - Tip: Given dueDate = 2026-05-25, today = 2026-05-20, isCompleted = true / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "CLEAR"
  - A completed task with no due date returns "CLEAR". `[Unit]`
    - Tip: Given dueDate = null, today = 2026-05-20, isCompleted = true / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "CLEAR"
  - An active task with no due date returns the empty string "". `[Unit]`
    - Tip: Given dueDate = null, today = 2026-05-20, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns ""
  - An active task due today returns "D-Day". `[Unit]`
    - Tip: Given dueDate = 2026-05-20, today = 2026-05-20, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-Day"
  - An active task due in N days returns "D-N". `[Unit]`
    - Tip: Given dueDate = 2026-05-25, today = 2026-05-20, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-5"
  - An active task overdue by N days returns "D+N". `[Unit]`
    - Tip: Given dueDate = 2026-05-15, today = 2026-05-20, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D+5"
  - An active task due in a three-digit number of days writes every digit: 100 days ahead returns "D-100". `[Unit]`
    - Tip: Given dueDate = 2026-08-28, today = 2026-05-20, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-100"
  - An active task overdue by a three-digit number of days writes every digit: 100 days overdue returns "D+100". `[Unit]`
    - Tip: Given dueDate = 2026-02-09, today = 2026-05-20, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D+100"
- Error cases
  - None. Every combination of a nullable `dueDate`, a `today` and a completion flag is valid input.
- Edge cases
  - Completion wins over a due date of today: a completed task due today returns "CLEAR", not "D-Day". `[Unit]`
    - Tip: Given dueDate = 2026-05-20, today = 2026-05-20, isCompleted = true / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "CLEAR"
  - Completion wins over an overdue date: a completed task overdue by 1 day returns "CLEAR", not "D+1". `[Unit]`
    - Tip: Given dueDate = 2026-05-19, today = 2026-05-20, isCompleted = true / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "CLEAR"
  - Completion wins over a due date a full month away: a completed task due in 31 days returns "CLEAR", not "D-31". `[Unit]`
    - Tip: Given dueDate = 2026-04-01, today = 2026-03-01, isCompleted = true / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "CLEAR"
  - One day before the D-Day boundary: an active task due tomorrow returns "D-1". `[Unit]`
    - Tip: Given dueDate = 2026-05-21, today = 2026-05-20, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-1"
  - One day after the D-Day boundary: an active task due yesterday returns "D+1". `[Unit]`
    - Tip: Given dueDate = 2026-05-19, today = 2026-05-20, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D+1"
  - Month end, inside the month: due on the last day of the month, one day ahead, returns "D-1". `[Unit]`
    - Tip: Given dueDate = 2026-01-31, today = 2026-01-30, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-1"
  - Month end, across the month: due on the first day of the next month, one day ahead, returns "D-1". `[Unit]`
    - Tip: Given dueDate = 2026-02-01, today = 2026-01-31, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-1"
  - Month end, two days across: due on the first day of the next month, two days ahead, returns "D-2". `[Unit]`
    - Tip: Given dueDate = 2026-02-01, today = 2026-01-30, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-2"
  - One-month mark, one day before it: due one day before the same date next month (30 days ahead) returns "D-30". `[Unit]`
    - Tip: Given dueDate = 2026-03-31, today = 2026-03-01, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-30"
  - One-month mark, at it: due on the same date next month (31 days ahead) returns "D-31", not "D-Day". `[Unit]`
    - Tip: Given dueDate = 2026-04-01, today = 2026-03-01, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-31"
  - One-month mark, one day after it: due one day after the same date next month (32 days ahead) returns "D-32", not "D-1". `[Unit]`
    - Tip: Given dueDate = 2026-04-02, today = 2026-03-01, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-32"
  - Overdue one-month mark, one day before it: 30 days overdue returns "D+30". `[Unit]`
    - Tip: Given dueDate = 2026-03-02, today = 2026-04-01, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D+30"
  - Overdue one-month mark, at it: overdue since the same date last month (31 days) returns "D+31", not "D-Day". `[Unit]`
    - Tip: Given dueDate = 2026-03-01, today = 2026-04-01, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D+31"
  - Overdue one-month mark, one day after it: 32 days overdue returns "D+32", not "D+1". `[Unit]`
    - Tip: Given dueDate = 2026-02-28, today = 2026-04-01, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D+32"
  - Crossing a 28-day February, 28 days ahead (end of February): returns "D-28". `[Unit]`
    - Tip: Given dueDate = 2026-02-28, today = 2026-01-31, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-28"
  - Crossing a 28-day February, 29 days ahead (into March): returns "D-29". `[Unit]`
    - Tip: Given dueDate = 2026-03-01, today = 2026-01-31, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-29"
  - Crossing a 28-day February, 30 days ahead: returns "D-30". `[Unit]`
    - Tip: Given dueDate = 2026-03-02, today = 2026-01-31, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-30"
  - Exactly one calendar month ahead counts every day: 31 days ahead returns "D-31", not "D-Day". `[Unit]`
    - Tip: Given dueDate = 2026-02-15, today = 2026-01-15, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-31"
  - Exactly one calendar month overdue counts every day: 31 days overdue returns "D+31", not "D-Day". `[Unit]`
    - Tip: Given dueDate = 2026-01-15, today = 2026-02-15, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D+31"
  - Overdue across a month end: 30 days overdue returns "D+30". `[Unit]`
    - Tip: Given dueDate = 2026-01-31, today = 2026-03-02, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D+30"
  - Year end, one day ahead: due on New Year's Day from New Year's Eve returns "D-1". `[Unit]`
    - Tip: Given dueDate = 2027-01-01, today = 2026-12-31, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-1"
  - Year end, two days ahead: due on New Year's Day from December 30 returns "D-2". `[Unit]`
    - Tip: Given dueDate = 2027-01-01, today = 2026-12-30, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-2"
  - Year end, one day overdue: due on New Year's Eve, seen on New Year's Day, returns "D+1". `[Unit]`
    - Tip: Given dueDate = 2026-12-31, today = 2027-01-01, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D+1"
  - Exactly one non-leap year ahead returns "D-365", not "D-Day". `[Unit]`
    - Tip: Given dueDate = 2027-01-01, today = 2026-01-01, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-365"
  - Exactly one leap year ahead returns "D-366", not "D-Day". `[Unit]`
    - Tip: Given dueDate = 2029-01-01, today = 2028-01-01, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-366"
  - Leap day, one day ahead: due on February 29 from February 28 of a leap year returns "D-1". `[Unit]`
    - Tip: Given dueDate = 2028-02-29, today = 2028-02-28, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-1"
  - Leap day, across it: due on March 1 from February 28 of a leap year returns "D-2". `[Unit]`
    - Tip: Given dueDate = 2028-03-01, today = 2028-02-28, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-2"
  - No leap day: due on March 1 from February 28 of a non-leap year returns "D-1". `[Unit]`
    - Tip: Given dueDate = 2027-03-01, today = 2027-02-28, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D-1"
  - Leap day, one day overdue: due on February 29, seen on March 1 of a leap year, returns "D+1". `[Unit]`
    - Tip: Given dueDate = 2028-02-29, today = 2028-03-01, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D+1"
  - Leap day, overdue across it: due on February 28, seen on March 1 of a leap year, returns "D+2". `[Unit]`
    - Tip: Given dueDate = 2028-02-28, today = 2028-03-01, isCompleted = false / When dueDateLabel(dueDate, today, isCompleted) is called / Then it returns "D+2"

**Undefined behavior** (optional)
- Dates so far apart that N does not fit in an `Int` (millions of years). The app's date picker cannot produce them, so their label is not specified.
