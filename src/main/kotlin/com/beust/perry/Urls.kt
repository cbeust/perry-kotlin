package com.beust.perry

import com.google.inject.Inject

class Urls @Inject constructor(private val properties: IConfig) {
    companion object {
        const val CYCLES = "/cycles"
        const val SUMMARIES = "/summaries"
        const val THANK_YOU_FOR_SUBMITTING = "/thankYouForSubmitting"
        const val PENDING = "/pending"
        const val API = "/api"
        const val LOGIN = "/login"
        const val COVERS = "/covers"
        const val RSS = "/rss"
        const val HOST: String = "https://www.perryrhodan.us"
        const val VERIFY: String = "/verifyUser"
        const val TEST: String = "/test"

        fun summaries(n: Any? = null) = f(SUMMARIES, n)
        fun cycles(n: Any? = null) = f(CYCLES, n)
        fun editSummary(n: Int) = summaries(n) + "/edit"
        fun verify(tempLink: String) = api("$VERIFY/$tempLink")

        private fun f(constant: String, n: Any? = null) = "$constant/$n"
        private fun api(s: String) = "$API$s"
    }

    fun summaries(n: Any? = null, fqdn: Boolean = false) = f(SUMMARIES, n, fqdn)

    fun cycles(n: Any? = null, fqdn: Boolean = false)  = f(CYCLES, n, fqdn)


    val host: String = properties.host

    private fun f(constant: String, n: Any? = null, fqdn: Boolean): String {
        val c = if (fqdn) host + constant
            else constant
        val result =
            if (n != null) {
                "$c/$n"
            } else {
                "/$c"
            }
        return result
    }
}
