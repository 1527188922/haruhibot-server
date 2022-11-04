package com.haruhi.botServer.instructions;

import com.haruhi.botServer.dto.gocq.response.Message;
import org.springframework.web.socket.WebSocketSession;

@FunctionalInterface
public interface IMatcher {

    boolean matches(WebSocketSession session,Message message);
}
