package org.springframework.web.socket.adapter.standard;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.Extension;
import jakarta.websocket.Session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.adapter.AbstractWebSocketSession;

@Slf4j
public class StandardWebSocketSession extends AbstractWebSocketSession<Session> {

	private final String id;

	@Nullable
	private URI uri;

	private final HttpHeaders handshakeHeaders;

	@Nullable
	private String acceptedProtocol;

	@Nullable
	private List<WebSocketExtension> extensions;

	@Nullable
	private Principal user;

	@Nullable
	private final InetSocketAddress localAddress;

	@Nullable
	private final InetSocketAddress remoteAddress;


	public StandardWebSocketSession(@Nullable HttpHeaders headers, @Nullable Map<String, Object> attributes,
			@Nullable InetSocketAddress localAddress, @Nullable InetSocketAddress remoteAddress) {

		this(headers, attributes, localAddress, remoteAddress, null);
	}

	public StandardWebSocketSession(@Nullable HttpHeaders headers, @Nullable Map<String, Object> attributes,
			@Nullable InetSocketAddress localAddress, @Nullable InetSocketAddress remoteAddress,
			@Nullable Principal user) {

		super(attributes);
		this.id = idGenerator.generateId().toString();
		headers = (headers != null ? headers : new HttpHeaders());
		this.handshakeHeaders = HttpHeaders.readOnlyHttpHeaders(headers);
		this.user = user;
		this.localAddress = localAddress;
		this.remoteAddress = remoteAddress;
		log.info("self object: {}",this);
	}


	@Override
	public String getId() {
		return this.id;
	}

	@Override
	@Nullable
	public URI getUri() {
		checkNativeSessionInitialized();
		return this.uri;
	}

	@Override
	public HttpHeaders getHandshakeHeaders() {
		return this.handshakeHeaders;
	}

	@Override
	public String getAcceptedProtocol() {
		checkNativeSessionInitialized();
		return this.acceptedProtocol;
	}

	@Override
	public List<WebSocketExtension> getExtensions() {
		Assert.state(this.extensions != null, "WebSocket session is not yet initialized");
		return this.extensions;
	}

	@Override
	public Principal getPrincipal() {
		return this.user;
	}

	@Override
	@Nullable
	public InetSocketAddress getLocalAddress() {
		return this.localAddress;
	}

	@Override
	@Nullable
	public InetSocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}

	@Override
	public void setTextMessageSizeLimit(int messageSizeLimit) {
		checkNativeSessionInitialized();
		getNativeSession().setMaxTextMessageBufferSize(messageSizeLimit);
	}

	@Override
	public int getTextMessageSizeLimit() {
		checkNativeSessionInitialized();
		return getNativeSession().getMaxTextMessageBufferSize();
	}

	@Override
	public void setBinaryMessageSizeLimit(int messageSizeLimit) {
		checkNativeSessionInitialized();
		getNativeSession().setMaxBinaryMessageBufferSize(messageSizeLimit);
	}

	@Override
	public int getBinaryMessageSizeLimit() {
		checkNativeSessionInitialized();
		return getNativeSession().getMaxBinaryMessageBufferSize();
	}

	@Override
	public boolean isOpen() {
		return getNativeSession().isOpen();
	}

	@Override
	public void initializeNativeSession(Session session) {
		super.initializeNativeSession(session);

		this.uri = session.getRequestURI();
		this.acceptedProtocol = session.getNegotiatedSubprotocol();

		List<Extension> standardExtensions = getNativeSession().getNegotiatedExtensions();
		if (!CollectionUtils.isEmpty(standardExtensions)) {
			this.extensions = new ArrayList<>(standardExtensions.size());
			for (Extension standardExtension : standardExtensions) {
				this.extensions.add(new StandardToWebSocketExtensionAdapter(standardExtension));
			}
			this.extensions = Collections.unmodifiableList(this.extensions);
		}
		else {
			this.extensions = Collections.emptyList();
		}

		if (this.user == null) {
			this.user = session.getUserPrincipal();
		}

	}

	@Override
	protected void sendTextMessage(TextMessage message) throws IOException {
		getNativeSession().getAsyncRemote().sendText(message.getPayload());
	}

	@Override
	protected void sendBinaryMessage(BinaryMessage message) throws IOException {
		getNativeSession().getAsyncRemote().sendBinary(message.getPayload());
	}

	@Override
	protected void sendPingMessage(PingMessage message) throws IOException {
		getNativeSession().getAsyncRemote().sendPing(message.getPayload());
	}

	@Override
	protected void sendPongMessage(PongMessage message) throws IOException {
		getNativeSession().getAsyncRemote().sendPong(message.getPayload());
	}

	@Override
	protected void closeInternal(CloseStatus status) throws IOException {
		getNativeSession().close(new CloseReason(CloseCodes.getCloseCode(status.getCode()), status.getReason()));
	}

	public String toString(){
		return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + "?id=" + id;
	}

}
