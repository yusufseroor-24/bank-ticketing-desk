package com.ga.bankdesk.notifications;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps one open live-notification connection (SseEmitter) per logged-in user.
 */

@Component
//track all connected users, check if it's currently listening to push notifications
public class SseEmitterRegistry {

    //maps a user's ID to their currently open connection (if any)
    //ConcurrentHashMap - a safer way to handle multiple requests at the same time
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    //connected users that are admins (they also receive the Kafka system events)
    private final Set<Long> adminIds = ConcurrentHashMap.newKeySet();

    public SseEmitter register(Long userId, boolean admin){
        SseEmitter emitter = new SseEmitter(0L); //stays open all the time
        emitters.put(userId, emitter);
        if (admin) {
            adminIds.add(userId);
        }

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));

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
            remove(userId, emitter);
        }
    }

    public void sendToAdmins(String eventName, Object data){
        for (Long adminId : adminIds) {
            sendToUser(adminId, eventName, data);
        }
    }

    //only removes the user if this is still their current connection
    private void remove(Long userId, SseEmitter emitter){
        if (emitters.remove(userId, emitter)) {
            adminIds.remove(userId);
        }
    }
}