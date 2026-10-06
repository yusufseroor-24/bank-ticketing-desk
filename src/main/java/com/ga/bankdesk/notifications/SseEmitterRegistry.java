package com.ga.bankdesk.notifications;



import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
//track all connected users, check if it's currently listening to push notifications
public class SseEmitterRegistry {

    //maps a user's ID to their currently open connection (if any)
    //ConcurrentHashMap - a safer way to handle multiple requests at the same time
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(Long userId){
        SseEmitter emitter = new SseEmitter(0L); //stays open all the time
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        return emitter;
    }

    public void sendToUser(Long userId, String eventName, Object data){
        SseEmitter emitter = emitters.get(userId);
        if(emitter == null){
            //(user not connected)
            return;
        }
        try{
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception e){
            emitters.remove(userId);
        }
    }
}
