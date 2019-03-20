package com.beust.perry

import com.google.inject.Inject

class Urls @Inject constructor(private val properties: TypedProperties) {
    companion object {
        const val CYCLES = "/cycles"
        const val SUMMARIES = "/summaries"
        const val THANK_YOU_FOR_SUBMITTING = "/thankYouForSubmitting"
        const val PENDING = "/pending"

        fun summaries(n: Any? = null) = f(SUMMARIES, n)
        fun cycles(n: Any? = null) = f(CYCLES, n)

        private fun f(constant: String, n: Any? = null) = "$constant/$n"
    }

    fun summaries(n: Any? = null, fqdn: Boolean = false) = f(SUMMARIES, n, fqdn)

    fun cycles(n: Any? = null, fqdn: Boolean = false)  = f(CYCLES, n, fqdn)

    private fun f(constant: String, n: Any? = null, fqdn: Boolean): String {
        val c = if (fqdn) properties.get(LocalProperty.HOST) + "/" + constant
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
