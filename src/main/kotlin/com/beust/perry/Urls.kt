package com.beust.perry

object Urls {
    const val SUMMARIES = "/summaries"
    fun summaries(n: Any? = null) = f(SUMMARIES, n)

    const val CYCLES = "/cycles"
    const val THANK_YOU_FOR_SUBMITTING = "/thankYouForSubmitting"

    const val PENDING = "/pending"
    fun cycles(n: Any? = null)  = f(CYCLES, n)

    private fun f(constant: String, n: Any? = null)  = if (n != null) "$constant/$n" else "/$constant"
}
