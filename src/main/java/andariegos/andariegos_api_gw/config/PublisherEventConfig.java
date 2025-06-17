package andariegos.andariegos_api_gw.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import andariegos.andariegos_api_gw.filters.ReportFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;


@Configuration
public class PublisherEventConfig {

    private static final Logger log = LoggerFactory.getLogger(PublisherEventConfig.class);

    @Value("${NAME_QUEUE}")
    private String queueName;

    @Bean
    public Queue queue() {
        return new Queue(queueName, true);
    }
    
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        log.info("inicializado ");
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public ApplicationRunner declareQueue(RabbitAdmin rabbitAdmin, Queue queue) {
        return args -> {
            rabbitAdmin.declareQueue(queue);
            System.out.println(" Queue '" + queue.getName() + "' declared manually by ApplicationRunner");
        };
    }
}
