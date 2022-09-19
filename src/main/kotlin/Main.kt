package tasklist

import java.io.File
import kotlinx.datetime.*
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory


class TaskList {
    private val taskList = mutableListOf<Task?>()
    private val taskFile = File("tasklist.json")

    private fun readAction(): String {
        while (true) {
            val actions = Actions.values().joinToString { it.action }
            println("Input an action ($actions):")

            val input = readln()
            for (enum in Actions.values()) {
                if (enum.action == input) return enum.action
            }
            println("The input action is invalid")
        }
    }

    private fun readPriority(): String {
        while (true) {
            val priorities = Priority.values().joinToString { it.str }
            println("Input the task priority ($priorities):")
            val input = readln().uppercase()
            for (enum in Priority.values()) {
                if (input == enum.str) {
                    return enum.color
                }
            }
        }
    }

    private fun readDate(): String {
        while (true) {
            try {
                println("Input the date (yyyy-mm-dd):")
                val (year, month, day) = readln().split("-").map { it.toInt() }
                LocalDate(year, month, day)

                var resYear = "$year"
                var resMonth = "$month"
                var resDay = "$day"

                if (year in 1..9) resYear = "000$year"
                if (year in 10..99) resYear = "00$year"
                if (year in 100..999) resYear = "0$year"
                if (month in 1..9) resMonth = "0$month"
                if (day in 1..9) resDay = "0$day"

                return ("$resYear-$resMonth-$resDay")
            } catch (e: Exception) {
                println("The input date is invalid")
            }
        }
    }

    private fun readTime(): String {
        while (true) {
            try {
                println("Input the time (hh:mm):")
                val (hour, minute) = readln().split(":").map { it.toInt() }
                if (hour !in 0..23) throw Exception()
                if (minute !in 0..59) throw Exception()

                var resHour = "$hour"
                var resMinute = "$minute"


                if (hour in 0..9 && resHour.length == 1) resHour = "0$hour"
                if (minute in 0..9 && resMinute.length == 1) resMinute = "0$minute"

                return ("$resHour:$resMinute")
            } catch (e: Exception) {
                println("The input time is invalid")
            }
        }
    }

    private fun readEdit(): String {
        while (true) {
            val edits = Edit.values().joinToString { it.value }
            println("Input a field to edit ($edits):")
            val input = readln()
            for (enum in Edit.values()) {
                if (enum.value == input) return enum.value
            }
            println("Invalid field")
        }
    }

    private fun compareTime(date: String, time: String): String {
        val givenDate = Instant.parse("${date}T${time}Z")
        val currentDate = Clock.System.now()

        if (currentDate.until(givenDate, DateTimeUnit.HOUR) < 0) return TimeTag.OVERDUE.color
        if (currentDate.until(givenDate, DateTimeUnit.DAY, TimeZone.UTC) == 0L) return TimeTag.TODAY.color
        return TimeTag.IN_TIME.color
    }

    private fun createTasks(priority: String, date: String, time: String): Task? {
        val taskInfo = mutableListOf<String>()
        val taskItems = mutableListOf<String>()
        println("Input a new task (enter a blank line to end):")

        while (true) {
            val task = readln().trim()

            if (task.isBlank() && taskItems.isEmpty()) {
                println("The task is blank")
                return null
            } else {
                if (task.isBlank()) {
                    return Task(taskInfo, taskItems)
                }
                if (taskInfo.isEmpty()) {
                    taskInfo.add(date)
                    taskInfo.add(time)
                    taskInfo.add(priority)
                }

                taskItems.add(task)
            }
        }
    }

    private fun printSingleTask(num: String, stamp: MutableList<String>, items: MutableList<String>) {
        val (date, time, priority) = stamp
        val timeTag = compareTime(date, time)

        val bodyWidth = 44
        val tasks = mutableListOf<MutableList<String>>()
        for (item in items) {
            val stringList = mutableListOf<String>()
            for (i in item.indices step bodyWidth) {
                if (i + bodyWidth <= item.length) {
                    stringList.add(item.substring(i, i + bodyWidth))
                } else {
                    stringList.add(
                        "${
                            item.substring(
                                i,
                                item.length
                            )
                        }${" ".repeat(bodyWidth - (item.length % bodyWidth))}"
                    )
                }
            }
            tasks.add(stringList)
        }

        for (i in tasks.indices) {
            for (j in 0 until tasks[i].size) {
                if (i == 0 && j == 0) {
                    println("| $num| $date | $time | $priority | $timeTag |${tasks[0][0]}|")
                    continue
                }
                val bodyTemplate = "|    |            |       |   |   |${tasks[i][j]}|"
                println(bodyTemplate)
            }
        }
    }

    private fun printList() {
        // table header
        println(
            """
            +----+------------+-------+---+---+--------------------------------------------+
            | N  |    Date    | Time  | P | D |                   Task                     |
            +----+------------+-------+---+---+--------------------------------------------+
        """.trimIndent()
        )

        for (i in taskList.indices) {
            val (taskInfo, taskItems) = taskList[i]!!
            var num = ""

            val itemDivider = "+----+------------+-------+---+---+--------------------------------------------+"

            if (i in 0 until 9) num = "${i + 1}  "
            if (i in 10 until 99) num = "${i + 1} "
            if (i > 99) num = "${i + 1}"

            printSingleTask(num, taskInfo, taskItems)
            println(itemDivider)
        }
    }

    private fun editTask() {
        while (true) {
            println("Input the task number (1-${taskList.size}):")
            try {
                val itemIndex = readln().toInt() - 1
                if (itemIndex !in taskList.indices) throw Exception()

                val (date, time, priority) = taskList[itemIndex]!!.taskInfo

                when (readEdit()) {
                    "priority" -> taskList[itemIndex]!!.taskInfo[2] = readPriority()
                    "date" -> taskList[itemIndex]!!.taskInfo[0] = readDate()
                    "time" -> taskList[itemIndex]!!.taskInfo[1] = readTime()
                    "task" -> {
                        val newTask = createTasks(priority, date, time)
                        taskList[itemIndex] = newTask
                    }
                }

                return println("The task is changed")
            } catch (e: Exception) {
                println("Invalid task number")
            }
        }
    }

    private fun deleteTask() {
        while (true) {
            println("Input the task number (1-${taskList.size}):")
            try {
                val itemIndex = readln().toInt() - 1
                if (itemIndex !in taskList.indices) throw Exception()

                taskList.removeAt(itemIndex)
                return println("The task is deleted")
            } catch (e: Exception) {
                println("Invalid task number")
            }
        }
    }

    private fun jsonConverter(convert: String) {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val type = Types.newParameterizedType(MutableList::class.java, Task::class.java)
        val taskListAdapter = moshi.adapter<MutableList<Task?>>(type)
        when (convert) {
            "to" -> {
                taskFile.writeText(taskListAdapter.toJson(taskList))
            }
            "from" -> {
                val jsonStr = taskFile.readText()
                val newTaskList = taskListAdapter.fromJson(jsonStr)
                for (task in newTaskList!!) {
                    taskList.add(task)
                }
            }
        }
    }

    private fun readFile() {
        jsonConverter("from")
    }

    private fun updateFile() {
        jsonConverter("to")
    }

    private fun isAnyTaskInList(): Boolean {
        return if (taskList.isNotEmpty()) {
            true
        } else {
            println("No tasks have been input")
            false
        }
    }

    fun start() {
        if (taskFile.exists()) readFile()
        while (true) {
            when (readAction()) {
                "add" -> {
                    val tasks = createTasks(readPriority(), readDate(), readTime())
                    if (tasks != null) taskList.add(tasks)
                    updateFile()
                }

                "print" -> if (isAnyTaskInList()) printList()
                "edit" -> {
                    if (isAnyTaskInList()) {
                        printList()
                        editTask()
                        updateFile()
                    }
                }

                "delete" -> {
                    if (isAnyTaskInList()) {
                        printList()
                        deleteTask()
                        updateFile()
                    }
                }

                "end" -> return end()
            }
        }
    }

    private fun end() {
        println("Tasklist exiting!")
    }

    data class Task(val taskInfo: MutableList<String>, val taskItems: MutableList<String>)

    enum class Colors(val color: String) {
        RED("\u001B[101m \u001B[0m"),
        YELLOW("\u001B[103m \u001B[0m"),
        GREEN("\u001B[102m \u001B[0m"),
        BLUE("\u001B[104m \u001B[0m")
    }

    enum class Actions(val action: String) {
        ADD("add"),
        PRINT("print"),
        EDIT("edit"),
        DELETE("delete"),
        END("end")
    }

    enum class Edit(val value: String) {
        PRIORITY("priority"),
        DATE("date"),
        TIME("time"),
        TASK("task")
    }

    enum class Priority(val str: String, val color: String) {
        CRITICAL("C", Colors.RED.color),
        HIGH("H", Colors.YELLOW.color),
        NORMAL("N", Colors.GREEN.color),
        LOW("L", Colors.BLUE.color),
    }

    enum class TimeTag(val str: String, val color: String) {
        IN_TIME("I", Colors.GREEN.color),
        TODAY("T", Colors.YELLOW.color),
        OVERDUE("O", Colors.RED.color)
    }
}

fun main() {
    val taskList = TaskList()

    taskList.start()
}
