# Spec: `TasksListBindings.kt`

> Post-conditions are the test oracle: write the expected behavior from intent, not from the code.
> Tag each post-condition [Unit] if its test runs on the JVM; otherwise tag it [Integration].
> End each valid case with `Tested by <test>` if an existing test checks it, or `Tip:` describing the test to write.

## `@BindingAdapter("app:dueDateLabel") fun setDueDateLabel(textView: TextView, task: Task?)`

**Intent**: Show the D-day label of `task` for today's date in the row's due-date `TextView` (`due_date_text` in `task_item.xml`). When the task has a label, the view is visible and shows exactly that label. When there is no label (an active task without a due date, or no task), the user sees no label at all. The label must follow the task's current state: rows in the task list are recycled and rebound when the user checks or unchecks a task, so every call must fully replace what an earlier call left on the same view. In the Tips below, "shows label X" means `textView.visibility` is `View.VISIBLE` and `textView.text.toString()` is exactly X, and "shows no label" means `textView.text.toString()` is "" or `textView.visibility` is not `View.VISIBLE` (emptying or hiding the view is an implementation choice; the user sees nothing either way).

**Pre-conditions**
- Called on the main thread with a real Android `TextView` (the test needs an Android `Context`, so it runs in `app/src/androidTest`).
- `task` may be `null` (data binding passes `null` before the row's task is set).
- "Today" is the device's current local date (`LocalDate.now()`); it cannot be injected. In every Tip below, `today` means `LocalDate.now()` read in the Given step, and due dates are built from it, for example `today.plusDays(1)`.
- The label text itself follows `specs/DueDateUtils.spec.md`; this spec checks that the view shows that label, and only that label, for the task it is currently bound to.

**Post-conditions**
- Success cases
  - A completed task with a due date shows "CLEAR". `[Integration]`
    - Tip: Given a new TextView with visibility View.GONE and task = Task("Title", "Description", isCompleted = true, dueDate = today.plusDays(3)) / When setDueDateLabel(textView, task) is called / Then the view shows label "CLEAR"
  - An active task due in N days shows "D-N". `[Integration]`
    - Tip: Given a new TextView with visibility View.GONE and task = Task("Title", "Description", isCompleted = false, dueDate = today.plusDays(3)) / When setDueDateLabel(textView, task) is called / Then the view shows label "D-3"
  - An active task overdue by N days shows "D+N". `[Integration]`
    - Tip: Given a new TextView with visibility View.GONE and task = Task("Title", "Description", isCompleted = false, dueDate = today.minusDays(3)) / When setDueDateLabel(textView, task) is called / Then the view shows label "D+3"
  - An active task with no due date shows no label. `[Integration]`
    - Tip: Given a new TextView and task = Task("Title", "Description", isCompleted = false, dueDate = null) / When setDueDateLabel(textView, task) is called / Then the view shows no label
  - A `null` task shows no label. `[Integration]`
    - Tip: Given a new TextView and task = null / When setDueDateLabel(textView, task) is called / Then the view shows no label
  - In the task list, checking an active task with no due date shows "CLEAR" in its row. `[Integration]`
    - Tip: Given the repository holds only Task("TITLE1", "DESCRIPTION1", isCompleted = false, id = "id1", dueDate = null) and TasksFragment is launched / When the complete_checkbox in the TITLE1 row is clicked / Then the due_date_text in the TITLE1 row shows label "CLEAR"
  - In the task list, checking an active task with a due date replaces its D-day label with "CLEAR". `[Integration]`
    - Tip: Given the repository holds only Task("TITLE1", "DESCRIPTION1", isCompleted = false, id = "id1", dueDate = today.plusDays(2)) and TasksFragment is launched, so the row shows "D-2" / When the complete_checkbox in the TITLE1 row is clicked / Then the due_date_text in the TITLE1 row shows label "CLEAR"
- Error cases
  - None. Every `Task`, including `null`, is valid input.
- Edge cases
  - At the D-Day boundary: an active task due today shows "D-Day". `[Integration]`
    - Tip: Given a new TextView with visibility View.GONE and task = Task("Title", "Description", isCompleted = false, dueDate = today) / When setDueDateLabel(textView, task) is called / Then the view shows label "D-Day"
  - One day before the D-Day boundary: an active task due tomorrow shows "D-1". `[Integration]`
    - Tip: Given a new TextView with visibility View.GONE and task = Task("Title", "Description", isCompleted = false, dueDate = today.plusDays(1)) / When setDueDateLabel(textView, task) is called / Then the view shows label "D-1"
  - One day after the D-Day boundary: an active task due yesterday shows "D+1". `[Integration]`
    - Tip: Given a new TextView with visibility View.GONE and task = Task("Title", "Description", isCompleted = false, dueDate = today.minusDays(1)) / When setDueDateLabel(textView, task) is called / Then the view shows label "D+1"
  - Completion boundary with no due date: a completed task with no due date shows "CLEAR"; the same task while active is the no-due-date Success case above and shows no label. `[Integration]`
    - Tip: Given a new TextView with visibility View.GONE and task = Task("Title", "Description", isCompleted = true, dueDate = null) / When setDueDateLabel(textView, task) is called / Then the view shows label "CLEAR"
  - Recycled view, label to no label: a view that showed a D-day label and is rebound to an active task with no due date shows no label. `[Integration]`
    - Tip: Given a TextView after setDueDateLabel(textView, Task("First", "Description", isCompleted = false, dueDate = today.plusDays(1))) left it showing "D-1" / When setDueDateLabel(textView, Task("Second", "Description", isCompleted = false, dueDate = null)) is called / Then the view shows no label
  - Recycled view, label to `null` task: a view that showed a label and is rebound to `null` shows no label. `[Integration]`
    - Tip: Given a TextView after setDueDateLabel(textView, Task("First", "Description", isCompleted = false, dueDate = today)) left it showing "D-Day" / When setDueDateLabel(textView, null) is called / Then the view shows no label
  - Recycled view, no label to label: a view with no label rebound to a task with a label shows the new label. `[Integration]`
    - Tip: Given a TextView after setDueDateLabel(textView, Task("First", "Description", isCompleted = false, dueDate = null)) left it with no label / When setDueDateLabel(textView, Task("Second", "Description", isCompleted = false, dueDate = today.minusDays(1))) is called / Then the view shows label "D+1"
  - Recycled view, task checked off: a view showing "D-1" rebound to the same task now completed shows "CLEAR". `[Integration]`
    - Tip: Given a TextView after setDueDateLabel(textView, Task("Title", "Description", isCompleted = false, id = "task1", dueDate = today.plusDays(1))) left it showing "D-1" / When setDueDateLabel(textView, Task("Title", "Description", isCompleted = true, id = "task1", dueDate = today.plusDays(1))) is called / Then the view shows label "CLEAR"
  - Recycled view, task with a due date unchecked: a view showing "CLEAR" rebound to the same task now active shows its D-day label again. `[Integration]`
    - Tip: Given a TextView after setDueDateLabel(textView, Task("Title", "Description", isCompleted = true, id = "task1", dueDate = today.plusDays(1))) left it showing "CLEAR" / When setDueDateLabel(textView, Task("Title", "Description", isCompleted = false, id = "task1", dueDate = today.plusDays(1))) is called / Then the view shows label "D-1"
  - Recycled view, task with no due date unchecked: a view showing "CLEAR" rebound to the same task now active with no due date shows no label; "CLEAR" must not stay. `[Integration]`
    - Tip: Given a TextView after setDueDateLabel(textView, Task("Title", "Description", isCompleted = true, id = "task1", dueDate = null)) left it showing "CLEAR" / When setDueDateLabel(textView, Task("Title", "Description", isCompleted = false, id = "task1", dueDate = null)) is called / Then the view shows no label
  - In the task list, unchecking a task with a due date brings its D-day label back. `[Integration]`
    - Tip: Given the repository holds only Task("TITLE1", "DESCRIPTION1", isCompleted = false, id = "id1", dueDate = today.plusDays(2)) and TasksFragment is launched / When the complete_checkbox in the TITLE1 row is clicked twice (check, then uncheck) / Then the due_date_text in the TITLE1 row shows label "D-2"
  - In the task list, unchecking a task with no due date removes "CLEAR". `[Integration]`
    - Tip: Given the repository holds only Task("TITLE1", "DESCRIPTION1", isCompleted = false, id = "id1", dueDate = null) and TasksFragment is launched / When the complete_checkbox in the TITLE1 row is clicked twice (check, then uncheck) / Then the due_date_text in the TITLE1 row shows no label

**Undefined behavior** (optional)
- A test that reads `LocalDate.now()` just before midnight and calls the function just after it sees a different "today". The function reads the clock itself, so this case cannot be pinned down; tests should not run across midnight.
- A row that stays bound while the date changes keeps its old label until it is bound again. Refreshing labels at midnight is outside this function.
