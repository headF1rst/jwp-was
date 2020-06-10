package webserver;

import http.controller.Controller;
import http.requests.HttpRequest;
import http.responses.HttpResponse;
import http.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import webserver.dispatcher.RequestDispatcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Optional;

public class RequestHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private final Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        logger.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            final HttpRequest httpRequest = new HttpRequest(in);
            final HttpResponse httpResponse = new HttpResponse(httpRequest, out);

            final String sessionId = Optional
                    .ofNullable(httpRequest.getCookie().getValue(SessionManager.SESSION_NAME))
                    .orElseGet(() -> {
                        final String newSessionId = SessionManager.createNewSession();
                        httpResponse.addHeader("Set-Cookie", String.format("%s=%s", SessionManager.SESSION_NAME, newSessionId));
                        return newSessionId;
                    });
            logger.debug("session id: {}", sessionId);

            final Controller controller = RequestDispatcher.dispatch(httpRequest);
            controller.service(httpRequest, httpResponse);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}
