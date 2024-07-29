package com.sirnuke.elusivebot.common

import com.sirnuke.elusivebot.common.impl.KafkaImpl
import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.KStream

import java.io.Closeable

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

typealias ConsumerCallback<T> = suspend (String, T) -> Unit
typealias ConsumerInit = (KStream<String, String>) -> Unit

/**
 * Thin wrapper Kafka that provides a coroutine friendly send and receive API.
 */
interface Kafka : Closeable {
    /**
     * Send a message on a Kafka channel.  FYI very thin wrapper for KafkaProducer.send.
     *
     * @param topic Kafka topic for the message
     * @param key Kafka key for message
     * @param message Serialized JSON string of the message
     * @param callback Callback when operation has completed
     */
    fun send(
        topic: String,
        key: String,
        message: String,
        callback: Callback
    )

    /**
     * Builder for constructing a Kafka interface instance.
     *
     * @param applicationId Client identifier to present to Kafka
     * @param bootstrap Bootstrap string for connecting to Kafka
     * @property scope Coroutine scope where consumer callbacks will run
     */
    class Builder(
        private val applicationId: String,
        private val bootstrap: String,
        val scope: CoroutineScope
    ) {
        /**
         * Internal builder for constructing KStream consumers. Public due to reified JSON data classes. Upstream users
         * are free to create their own custom topology, if the public API is insufficient.
         */
        val streamsBuilder = StreamsBuilder()

        /**
         * Register a new consumer topic.
         *
         * @param topic Kafka topic to consume
         * @param callback Lambda to call when a message is received
         * @property T Schema data class to deserialize from JSON
         * @return This builder instance for function chaining
         */
        inline fun <reified T> registerConsumer(topic: String, crossinline callback: ConsumerCallback<T>): Builder =
            registerConsumer(topic, {}, callback)

        /**
         * Register a new consumer topic with extra configuration of the KStream instance.
         *
         * @param topic Kafka topic to listen on
         * @param init Lambda for performing extra configuration of the KStream instance
         * @param callback Lambda to call when a message is received
         * @property T Schema data class to deserialize from JSON
         * @return This builder instance for function chaining
         */
        inline fun <reified T> registerConsumer(
            topic: String,
            init: ConsumerInit,
            crossinline callback: ConsumerCallback<T>
        ): Builder {
            val consumer: KStream<String, String> = streamsBuilder.stream(topic)
            init(consumer)

            consumer.foreach { key, raw ->
                val message: T = Json.decodeFromString(raw)
                scope.launch { callback(key, message) }
            }
            return this
        }

        /**
         * Complete configuration and build the Kafka instance.
         *
         * @return The newly started Kafka wrapping interface instance
         */
        fun construct(): Kafka = KafkaImpl(applicationId, bootstrap, streamsBuilder)
    }
}
