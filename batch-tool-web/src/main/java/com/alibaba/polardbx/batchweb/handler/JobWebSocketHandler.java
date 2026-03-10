package com.alibaba.polardbx.batchweb.handler;

import com.alibaba.polardbx.batchweb.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;

/**
 * Job WebSocket 处理器
 */
@Slf4j
@Component
public class JobWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private WebSocketService webSocketService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        webSocketService.registerSession(sessionId, session);

        // 从 URI 获取 jobId
        URI uri = session.getUri();
        if (uri != null) {
            String path = uri.getPath();
            String jobId = extractJobId(path);
            if (jobId != null) {
                webSocketService.subscribeJob(sessionId, jobId);
                log.info("WebSocket connection established: sessionId={}, jobId={}", sessionId, jobId);
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received message from {}: {}", session.getId(), payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();

        // 从 URI 获取 jobId
        URI uri = session.getUri();
        if (uri != null) {
            String path = uri.getPath();
            String jobId = extractJobId(path);
            if (jobId != null) {
                webSocketService.unsubscribeJob(sessionId, jobId);
            }
        }

        webSocketService.unregisterSession(sessionId);
        log.info("WebSocket connection closed: sessionId={}, status={}", sessionId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error: sessionId={}", session.getId(), exception);
        webSocketService.unregisterSession(session.getId());
    }

    private String extractJobId(String path) {
        // 路径格式: /ws/job/{jobId}
        String[] parts = path.split("/");
        if (parts.length >= 4) {
            return parts[3];
        }
        return null;
    }
}
