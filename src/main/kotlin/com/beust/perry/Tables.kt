package com.beust.perry

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column
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
    val title = varchar("title", 80).nullable()
    val author = varchar("author", 60).nullable()
    val published = date("published").nullable()
    val germanFile = varchar("german_file", 100).nullable()
}

object PendingSummaries: Table("pending") {
    val id: Column<Int> = integer("id").autoIncrement().primaryKey()
    val number = integer("number")
    val germanTitle = varchar("german_title", 80).nullable()
    val englishTitle = varchar("english_title", 80)
    val authorName = varchar("author_name", 60)
    val authorEmail = varchar("author_email", 60).nullable()
    val summary = text("summary")
    val dateSummary = varchar("date_summary", 40)
}

object Summaries: IntIdTable(columnName = "number") {
    val number = integer("number")
    val englishTitle = varchar("english_title", 80)
    val authorName = varchar("author_name", 60)
    val authorEmail = varchar("author_email", 60).nullable()
    val date = varchar("date", 40).nullable()
    val summary = text("summary")
    val time = varchar("time", 20).nullable()
}

object SummariesFr: IntIdTable(columnName = "number") {
    val number = integer("number")
    val englishTitle = varchar("english_title", 80)
    val authorName = varchar("author_name", 60)
    val authorEmail = varchar("author_email", 60)
    val date = varchar("date", 40)
    val summary = text("summary")
    val time = varchar("time", 20).nullable()
}

object Users: Table(name = "users") {
    val login = varchar("login", 40)
    val name = varchar("name", 80)
    val level = integer("level")
    val email = varchar("email", 60)
}
