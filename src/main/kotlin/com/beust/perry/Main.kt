package com.beust.perry

import com.beust.koolaid.DemoApp

fun main(args: Array<String>) {
    org.jetbrains.exposed.sql.Database.connect(
            "jdbc:mysql://localhost:3306/perry?user=root&password=cedricbeust",
            driver = "com.mysql.jdbc.Driver",
            user = "root", password = "cedricbeust")
    DemoApp().run(*args)
}