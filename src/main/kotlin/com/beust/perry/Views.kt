package com.beust.perry

import com.google.inject.Inject
import io.dropwizard.views.View

@Suppress("unused", "MemberVisibilityCanBePrivate", "CanBeParameter")
class CyclesView(val cycles: List<Cycle>, val recentSummaries: List<ShortSummary>, val summaryCount: Int,
        val bookCount: Int, val username: String?) : View("cycles.mustache") {
    val percentage: Int = summaryCount * 100 / bookCount
}

@Suppress("unused", "MemberVisibilityCanBePrivate", "CanBeParameter")
class CycleView(val cycle: Cycle, private val passedBooks: List<BookFromDao>,
        private val summaries: List<SummaryFromDao>, val username: String?) : View("cycle.mustache") {
    class SmallBook(val number: Int, val germanTitle: String?, val englishTitle: String?, val bookAuthor: String?,
            val href: String)

    val books = arrayListOf<SmallBook>()

    init {
        val summaryMap = hashMapOf<Int, SummaryFromDao>()
        summaries.forEach { summaryMap[it.number] = it}
        passedBooks.forEach { book ->
            val summary = summaryMap[book.number]
            books.add(SmallBook(book.number, book.germanTitle, summary?.englishTitle, book.author,
                    Urls.SUMMARIES + "/${book.number}"))
        }

    }
}

@Suppress("unused")
class SummaryView(val username: String?) : View("summary.mustache")

@Suppress("unused")
class EditSummaryView(val summary: Summary?, val username: String?) : View("editSummary.mustache")

class ThankYouForSubmittingView: View("thankYouForSubmitting.mustache")

class LoginView: View("login.mustache")

class RssView @Inject constructor(private val summariesDao: SummariesDao, private val urls: Urls,
        private val booksDao: BooksDao)
    : View("rss.mustache")
{
    class Item(val number: Int ,val englishTitle: String, val url: String, val germanTitle: String?, val date: String)
    val items: List<Item>
        get() {
            val result = summariesDao.findRecentSummaries(10).map {
                val book = booksDao.findBook(it.number)
                Item(it.number, it.englishTitle, urls.summaries(it.number, fqdn = true), book?.germanTitle, it.date)
            }
            return result
        }
}