package com.ga.bankdesk.cucumber;

import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/***
 * listens to what ticket-service publishes, so scenarios can check it
 */

@Component
@Profile("cucumber")
public class KafkaProbe {

    private static final String AS_STRING = "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer";

    public final List<String> emailEvents = new CopyOnWriteArrayList<>();
    public final List<String> liveEvents = new CopyOnWriteArrayList<>();
    public final List<String> ticketEvents = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = "email-events", groupId = "cucumber-probe", properties = AS_STRING)
    void email(String json) { emailEvents.add(json); }

    @KafkaListener(topics = "live-events", groupId = "cucumber-probe", properties = AS_STRING)
    void live(String json) { liveEvents.add(json); }

    @KafkaListener(topics = "ticket-events", groupId = "cucumber-probe", properties = AS_STRING)
    void ticket(String json) { ticketEvents.add(json); }
}