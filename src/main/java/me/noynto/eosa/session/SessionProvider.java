package me.noynto.eosa.session;

import me.noynto.eosa.shared.SessionId;

import java.util.stream.Stream;

public interface SessionProvider {

    Stream<SessionId> readIds();

    Session read(SessionId sessionId) throws UnknownSession;

    Session write(Session session);

    class UnknownSession extends Exception {
        private final SessionId sessionId;

        public UnknownSession(SessionId sessionId) {
            this.sessionId = sessionId;
        }

        public SessionId getSessionId() {
            return sessionId;
        }
    }

}
