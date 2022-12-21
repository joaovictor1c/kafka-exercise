package br.com.alura.ecommerce;

import br.com.alura.ecommerce.gson.GsonDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.Closeable;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

public class KafkaService<T> implements Closeable {

    private final KafkaConsumer<String, T> consumer;
    private final ConsumerFunction parse;

    KafkaService(String groupId, String topic, ConsumerFunction parse, Class<T> type,  Map<String,String> configs) {
       this(groupId, parse, type, configs);
        consumer.subscribe(Collections.singleton(topic));
    }
    KafkaService(String groupId, Pattern pattern, ConsumerFunction parse, Class<T> type,  Map<String,String> configs) {
        this(groupId, parse, type, configs);
        consumer.subscribe(pattern);
    }

    private KafkaService(String groupId, ConsumerFunction parse, Class<T> type,  Map<String,String> configs) {
        this.parse = parse;
        this.consumer = new KafkaConsumer<>(properties(groupId, type, configs));
    }


    void run() {
        while (true) {
            //verifica se tem registro em um pull durante um periodo
            var records = consumer.poll(Duration.ofMillis(100));
            if(!records.isEmpty()) {
                for (var record : records) {
                    parse.consume(record);
                }
            }
        }
    }

    private Properties properties(String groupId, Class<T> type, Map<String,String> configs) {
        Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, GsonDeserializer.class.getName());
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1");
        props.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
        props.setProperty(GsonDeserializer.TYPE_CONFIG, type.getName());
        props.putAll(configs);
        return props;
    }

    @Override
    public void close() {
        consumer.close();
    }
}
