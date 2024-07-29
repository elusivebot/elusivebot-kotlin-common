package com.sirnuke.elusivebot.common.impl

import com.sirnuke.elusivebot.common.Kafka
import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Implementation for Kafka wrapper. You probably want the Kafka interface, one package up from this.
 *
 * @param applicationId Client identifier to present to Kafka
 * @param bootstrap Bootstrap string for connecting to Kafka
 * @param streamsBuilder Configured streams builder for consuming topics
 */
internal class KafkaImpl(
    applicationId: String,
    bootstrap: String,
    streamsBuilder: StreamsBuilder
) : Kafka {
    private val producer: KafkaProducer<String, String>
    private val streams: KafkaStreams
    private val running = AtomicBoolean(true)

    init {
        val producerConfig = mapOf(
            "client.id" to applicationId,
            "bootstrap.servers" to bootstrap,
            "key.serializer" to "org.apache.kafka.common.serialization.StringSerializer",
            "value.serializer" to "org.apache.kafka.common.serialization.StringSerializer"
        )

        val consumerConfig = StreamsConfig(
            mapOf<String, Any>(
                StreamsConfig.APPLICATION_ID_CONFIG to applicationId,
                StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrap,
                StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG to Serdes.String().javaClass.name,
                StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG to Serdes.String().javaClass.name
            )
        )

        producer = KafkaProducer(producerConfig)
        streams = KafkaStreams(streamsBuilder.build(), consumerConfig)
        streams.start()
    }

    override fun send(
        topic: String,
        key: String,
        message: String,
        callback: Callback
    ) {
        producer.send(ProducerRecord(topic, key, message), callback)
    }

    override fun close() {
        if (running.getAndSet(false)) {
            producer.close()
            streams.close()
        }
    }
}
