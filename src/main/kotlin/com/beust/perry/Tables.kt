package com.beust.perry

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Table

object Cycles: IntIdTable(columnName = "number") {
    val number = integer("number")
    val germanTitle = varchar("german_title", 80)
    val englishTitle = varchar("english_title", 80)
    val shortTitle = varchar("short_title", 40)
    val start = integer("start")
    val end = integer("end")
}

object Hefte: IntIdTable(columnName = "number") {
    val number = integer("number")
    val title = varchar("title", 80)
    val author = varchar("author", 60)
    val published = date("published")
    val germanFile = varchar("german_file", 100).nullable()
}

object PendingSummaries: IntIdTable() {
    val number = integer("number")
    val germanTitle = varchar("german_title", 80)
    val author = varchar("author", 60)
    val published = varchar("published", 60)
    val englishTitle = varchar("english_title", 80)
    val authorName = varchar("author_name", 60)
    val authorEmail = varchar("author_email", 60)
    val dateSummary = varchar("date_summary", 40)
    val summary = text("summary")
}

object Summaries: IntIdTable(columnName = "number") {
    val number = integer("number")
    val englishTitle = PendingSummaries.varchar("english_title", 80)
    val authorName = varchar("author_name", 60)
    val authorEmail = varchar("author_email", 60)
    val date = varchar("date", 40)
    val summary = text("summary")
    val time = varchar("time", 20)
}

object SummariesFr: IntIdTable(columnName = "number") {
    val number = integer("number")
    val englishTitle = PendingSummaries.varchar("english_title", 80)
    val authorName = varchar("author_name", 60)
    val authorEmail = varchar("author_email", 60)
    val date = varchar("date", 40)
    val summary = text("summary")
    val time = varchar("time", 20)
}

object Users: Table(name = "name") {
    val login = varchar("login", 40)
    val name = varchar("name", 80)
    val level = integer("level")
    val email = varchar("email", 60)
}
