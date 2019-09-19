package com.arthurfritz.labelprinter.service;

import com.arthurfritz.labelprinter.dto.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class SimpleWsHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(SimpleWsHandler.class);

    private final String urlWs;
    private final PrinterService printService;
    private WebSocketConnectionManager webSocketManager;
    private PingJob pingJob;
    private ScheduledExecutorService pingScheduled;
    private WebSocketSession session;


    public SimpleWsHandler(@Value("${url.socket.label}") String urlWs, @Autowired PrinterService printerService) {
        this.urlWs = urlWs;
        this.printService = printerService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        this.session = session;
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        Message payload;
        try {
            if(!"PING".equalsIgnoreCase(message.getPayload().toString())) {
                payload = new ObjectMapper().readValue(message.getPayload().toString(), Message.class);
                printService.printMessage(payload);
            }
        } catch (Exception ex) {
            logger.warn("Mensagem no formato inválido : {} - {}", message.getPayload(), ex.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        session.close();
        newConnectionWs();
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }


    private void pingAtRegularIntervals() {
        pingJob = new PingJob();
        if (pingScheduled != null) {
            pingScheduled.shutdownNow();
        }
        pingScheduled = Executors.newSingleThreadScheduledExecutor();
        pingScheduled.scheduleAtFixedRate(pingJob, 5L, 10L, TimeUnit.SECONDS);
    }

    @PostConstruct
    protected void newConnectionWs() {
        webSocketManager = new WebSocketConnectionManager(
                new StandardWebSocketClient(),
                this,
                this.urlWs);
        webSocketManager.start();
        pingAtRegularIntervals();
    }

    @PreDestroy
    public void destroy() {
        if (pingScheduled != null) {
            pingScheduled.shutdownNow();
        }
    }

    class PingJob implements Runnable {

        @Override
        public void run() {
            try {
                logger.debug("Pinging WebSocket...");
                session.sendMessage(new TextMessage("PING"));
            } catch (Exception e) {
                logger.error("Error pinging WebSocket");
                if (!isWebSocketSessionOpen()) {
                    try {
                        webSocketManager.stop();
                    } catch (Exception innerException) {
                        logger.error("Error try closing websocket after failed ping. Exception:", innerException);
                    }
                }
                pingJob = null;
                if (pingScheduled != null) {
                    pingScheduled.shutdownNow();
                }
                pingScheduled = null;
                newConnectionWs();
            }
        }

        boolean isWebSocketSessionOpen() {
            return pingJob != null && session != null && session.isOpen();
        }
    }
}
