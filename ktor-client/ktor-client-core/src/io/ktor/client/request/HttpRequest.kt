package io.ktor.client.request

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.response.*
import io.ktor.client.utils.*
import io.ktor.content.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.experimental.*

/**
 * A request for [HttpClient], first part of [HttpClientCall].
 */
interface HttpRequest : HttpMessage {
    /**
     * The associated [HttpClientCall] containing both
     * the underlying [HttpClientCall.request] and [HttpClientCall.response].
     */
    val call: HttpClientCall

    /**
     * The [HttpMethod] or HTTP VERB used for this request.
     */
    val method: HttpMethod

    /**
     * The [Url] representing the endpoint and the uri for this request.
     */
    val url: Url

    /**
     * Typed [Attributes] associated to this request serving as a lightweight container.
     */
    val attributes: Attributes

    /**
     * A [Job] representing the process of this request.
     */
    val executionContext: Job

    val content: OutgoingContent

    /**
     * Actually executes / sends the request
     * including the request headers and the payload.
     */
    suspend fun execute(): HttpResponse
}

/**
 * Class for building [HttpRequestData].
 */
class HttpRequestBuilder : HttpMessageBuilder {
    /**
     * [URLBuilder] to configure the URL for this request.
     */
    val url = URLBuilder()

    /**
     * [HttpMethod] used by this request. [HttpMethod.Get] by default.
     */
    var method: HttpMethod = HttpMethod.Get

    /**
     * [HeadersBuilder] to configure the headers for this request.
     */
    override val headers = HeadersBuilder()

    /**
     * The [body] for this request. Initially [EmptyContent].
     */
    var body: Any = EmptyContent

    /**
     * A deferred used to control the execution of this request.
     */
    val executionContext: CompletableDeferred<Unit> = CompletableDeferred<Unit>()

    fun build(): HttpRequestData = HttpRequestData(
            url.build(), method, headers.build(), body, executionContext
    )

    companion object
}

/**
 * Actual data of the [HttpRequest], including [url], [method], [headers], [body] and [executionContext].
 * Built by [HttpRequestBuilder].
 */
class HttpRequestData(
        val url: Url,
        val method: HttpMethod,
        val headers: Headers,
        val body: Any,
        val executionContext: CompletableDeferred<Unit>
)

/**
 * Executes a [block] that configures the [HeadersBuilder] associated to this request.
 */
fun HttpRequestBuilder.headers(block: HeadersBuilder.() -> Unit): HeadersBuilder = headers.apply(block)

/**
 * Mutates [this] copying all the data from another [builder] using it as base.
 */
fun HttpRequestBuilder.takeFrom(builder: HttpRequestBuilder): HttpRequestBuilder {
    method = builder.method
    body = builder.body
    url.takeFrom(builder.url)
    headers.appendAll(builder.headers)

    return this
}

/**
 * Executes a [block] that configures the [URLBuilder] associated to this request.
 */
fun HttpRequestBuilder.url(block: URLBuilder.() -> Unit): Unit = block(url)

/**
 * Executes a [block] that configures the [URLBuilder] associated to this request.
 */
operator fun HttpRequestBuilder.Companion.invoke(block: URLBuilder.() -> Unit): HttpRequestBuilder =
        HttpRequestBuilder().apply { url(block) }

/**
 * Sets the [url] using the specified [scheme], [host], [port] and [path].
 */
fun HttpRequestBuilder.url(
        scheme: String = "http", host: String = "localhost", port: Int = 80, path: String = "/",
        block: URLBuilder.() -> Unit = {}
): Unit {
    url.apply {
        protocol = URLProtocol.createOrDefault(scheme, port)
        this.host = host
        this.port = port
        encodedPath = path
        block(url)
    }
}

/**
 * Constructs a [HttpRequestBuilder] from URL information: [scheme], [host], [port] and [path]
 * and optionally further configures it using [block].
 */
operator fun HttpRequestBuilder.Companion.invoke(
        scheme: String = "http", host: String = "localhost", port: Int = 80, path: String = "/",
        block: URLBuilder.() -> Unit = {}
): HttpRequestBuilder = HttpRequestBuilder().apply { url(scheme, host, port, path, block) }

/**
 * Sets the [HttpRequestBuilder.url] from [url].
 */
fun HttpRequestBuilder.url(url: java.net.URL): Unit = this.url.takeFrom(url)

/**
 * Constructs a [HttpRequestBuilder] from [url].
 */
operator fun HttpRequestBuilder.Companion.invoke(url: java.net.URL): HttpRequestBuilder =
        HttpRequestBuilder().apply { url(url) }
