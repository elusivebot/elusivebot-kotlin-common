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
 * @param consumer Consumer interface instance
 * @param producer Producer interface instance
 */
internal class KafkaImpl(private val consumer: Kafka.Consumer, private val producer: Kafka.Producer) : Kafka {
    private val running = AtomicBoolean(true)
    override fun send(
        topic: String,
        key: String,
        message: String,
        callback: Callback
    ) =
        producer.send(topic, key, message, callback)

    override fun close() {
        if (running.getAndSet(false)) {
            producer.close()
            consumer.close()
        }
    }

    /**
     * Implementation of the consumer API.
     *
     * @param applicationId Identifier to pass to Kafka
     * @param bootstrap Bootstrap string for accessing Kafka
     * @param streamsBuilder Topology for consumer topics
     */
    internal class ConsumerImpl(
        applicationId: String,
        bootstrap: String,
        streamsBuilder: StreamsBuilder
    ) : Kafka.Consumer {
        private val streams: KafkaStreams

        init {
            val consumerConfig = StreamsConfig(
                mapOf<String, Any>(
                    StreamsConfig.APPLICATION_ID_CONFIG to applicationId,
                    StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrap,
                    StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG to Serdes.String().javaClass.name,
                    StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG to Serdes.String().javaClass.name
                )
            )

            streams = KafkaStreams(streamsBuilder.build(), consumerConfig)
            streams.start()
        }

        override fun close() {
            streams.close()
        }
    }

    /**
     * Implementation of the producer API.
     *
     * @param applicationId Identifier to pass to Kafka
     * @param bootstrap Bootstrap string for accessing Kafka
     */
    internal class ProducerImpl(applicationId: String, bootstrap: String) : Kafka.Producer {
        private val producer: KafkaProducer<String, String>

        init {
            val producerConfig = mapOf(
                "client.id" to applicationId,
                "bootstrap.servers" to bootstrap,
                "key.serializer" to "org.apache.kafka.common.serialization.StringSerializer",
                "value.serializer" to "org.apache.kafka.common.serialization.StringSerializer"
            )
            producer = KafkaProducer(producerConfig)
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
            producer.close()
        }
    }
}
