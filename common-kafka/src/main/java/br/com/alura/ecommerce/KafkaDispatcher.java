package br.com.alura.ecommerce;

import br.com.alura.ecommerce.gson.GsonSerializer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.Closeable;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

class KafkaDispatcher<T> implements Closeable {

    private final KafkaProducer<String, T> producer;

    KafkaDispatcher() {
        this.producer = new KafkaProducer<>(properties());
    }

    void send(String topic, String key, T value) throws ExecutionException, InterruptedException {
        var record = new ProducerRecord<>(topic, key, value);

        Callback callback = (data, e) -> {
            if (e != null) {
                e.printStackTrace();
                return;
            }
            System.out.println("sucesso enviando " + data.topic() + " ::: Partition " + data.partition() +
                    " :::Offset " + data.offset() + " :::TimeStamp" + data.timestamp());
        };

        producer.send(record, callback).get();
    }

    private static Properties properties() {
        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, GsonSerializer.class.getName());

        return props;
    }

    @Override
    public void close() throws IOException {
        producer.close();
    }

    // é possivel criar varias partitions e o kafka se autobalanceia para gerenciar os novos consumers
    // porem, pode acontecer do kafka se rebalancear e os eventos que estavam aguardando para serem consumidos dar problema
    // para isso nao acontecer é necessario ter um maior controle dos commits que o kafka faz.
    // para resolver é possivel configurar para o consumer commitar a cada evento.
}
