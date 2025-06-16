package andariegos.andariegos_api_gw.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Queue;



@Configuration
public class PublisherEventConfig {


    @Value("${NAME_QUEUE}")
    private String queueName;

    @Bean
    public Queue queue() {
        return new Queue(queueName, true);
    }
    
}
