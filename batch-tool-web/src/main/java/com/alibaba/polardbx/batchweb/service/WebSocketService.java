package com.alibaba.polardbx.batchweb.service;

import com.alibaba.polardbx.batchweb.dto.JobProgress;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 服务
 */
@Slf4j
@Service
public class WebSocketService {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Map<String, WebSocketSession>> jobSubscribers = new ConcurrentHashMap<>();

    /**
     * 注册会话
     */
    public void registerSession(String sessionId, WebSocketSession session) {
        sessions.put(sessionId, session);
        log.debug("Session registered: {}", sessionId);
    }

    /**
     * 取消注册会话
     */
    public void unregisterSession(String sessionId) {
        sessions.remove(sessionId);
        // 从任务订阅中移除
        jobSubscribers.values().forEach(subs -> subs.remove(sessionId));
        log.debug("Session unregistered: {}", sessionId);
    }

    /**
     * 订阅任务
     */
    public void subscribeJob(String sessionId, String jobId) {
        jobSubscribers.computeIfAbsent(jobId, k -> new ConcurrentHashMap<>())
                .put(sessionId, sessions.get(sessionId));
        log.debug("Session {} subscribed to job {}", sessionId, jobId);
    }

    /**
     * 取消订阅任务
     */
    public void unsubscribeJob(String sessionId, String jobId) {
        Map<String, WebSocketSession> subs = jobSubscribers.get(jobId);
        if (subs != null) {
            subs.remove(sessionId);
            if (subs.isEmpty()) {
                jobSubscribers.remove(jobId);
            }
        }
        log.debug("Session {} unsubscribed from job {}", sessionId, jobId);
    }

    /**
     * 广播进度
     */
    public void broadcastProgress(String jobId, JobProgress progress) {
        Map<String, WebSocketSession> subs = jobSubscribers.get(jobId);
        if (subs == null || subs.isEmpty()) {
            return;
        }

        String message = JSON.toJSONString(Map.of("type", "progress", "data", progress));

        subs.forEach((sessionId, session) -> {
            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("Failed to send progress to session {}", sessionId, e);
                }
            }
        });
    }

    /**
     * 广播日志
     */
    public void broadcastLog(String jobId, String level, String message) {
        Map<String, WebSocketSession> subs = jobSubscribers.get(jobId);
        if (subs == null || subs.isEmpty()) {
            return;
        }

        Map<String, Object> logData = Map.of(
                "jobId", jobId,
                "level", level,
                "message", message,
                "timestamp", LocalDateTime.now().toString()
        );
        String logMessage = JSON.toJSONString(Map.of("type", "log", "data", logData));

        subs.forEach((sessionId, session) -> {
            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(logMessage));
                } catch (IOException e) {
                    log.error("Failed to send log to session {}", sessionId, e);
                }
            }
        });
    }
}
