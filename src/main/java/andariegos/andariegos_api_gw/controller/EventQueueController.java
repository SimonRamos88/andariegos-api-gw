package andariegos.andariegos_api_gw.controller;

import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import andariegos.andariegos_api_gw.filters.EventRegistrationFilter;
import andariegos.andariegos_api_gw.service.PublisherEventQueue;



@RestController
@RequestMapping("api/create")
public class EventQueueController {
    
    private static final Logger log = LoggerFactory.getLogger(EventRegistrationFilter.class);

    @Autowired
	private PublisherEventQueue publisher;
	
	@PostMapping()
    public ResponseEntity<String> publishEvent(@RequestBody String eventBody) {
        publisher.send(eventBody);
        return ResponseEntity.ok("Message published successfully");
    }
}
