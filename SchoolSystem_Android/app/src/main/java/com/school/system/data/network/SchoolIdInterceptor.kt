package com.school.system.data.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that adds the `x-school-id` header to every request.
 * The school id is provided by a lambda so it can be read dynamically (e.g., from DB or preferences).
 */
class SchoolIdInterceptor(private val schoolIdProvider: () -> String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val request = original.newBuilder()
            .addHeader("x-school-id", schoolIdProvider())
            .build()
        return chain.proceed(request)
    }
}
