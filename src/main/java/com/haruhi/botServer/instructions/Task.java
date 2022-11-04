package com.haruhi.botServer.instructions;

import com.haruhi.botServer.dto.gocq.response.Message;
import org.springframework.web.socket.WebSocketSession;

public interface Task extends Runnable {

   void run(WebSocketSession session, Message message);
}
