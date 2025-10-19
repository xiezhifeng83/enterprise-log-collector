package com.logcollector.collector.ssh;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages SSH connections to remote servers using JSch.
 *
 * Provides connection pooling and session management.
 */
@Component
@Slf4j
public class SSHConnectionManager {

    @Value("${log-collection.ssh.timeout:60000}")
    private int timeout;

    private final JSch jsch = new JSch();
    private final Map<String, Session> sessionPool = new ConcurrentHashMap<>();

    /**
     * Creates or retrieves an SSH session for the given server.
     *
     * @param hostname server hostname
     * @param port SSH port
     * @param username SSH username
     * @param password SSH password
     * @return connected SSH session
     * @throws JSchException if connection fails
     */
    public Session connect(String hostname, int port, String username, String password) throws JSchException {
        String key = createKey(hostname, port, username);

        // Check if session exists and is connected
        Session session = sessionPool.get(key);
        if (session != null && session.isConnected()) {
            log.debug("Reusing existing SSH session for {}", hostname);
            return session;
        }

        // Create new session
        log.info("Creating new SSH connection to {}@{}:{}", username, hostname, port);
        session = jsch.getSession(username, hostname, port);
        session.setPassword(password);

        // Disable strict host key checking (use carefully in production)
        session.setConfig("StrictHostKeyChecking", "no");
        session.setTimeout(timeout);

        session.connect();
        sessionPool.put(key, session);

        log.info("SSH connection established to {}", hostname);
        return session;
    }

    /**
     * Disconnects an SSH session.
     *
     * @param hostname server hostname
     * @param port SSH port
     * @param username SSH username
     */
    public void disconnect(String hostname, int port, String username) {
        String key = createKey(hostname, port, username);
        Session session = sessionPool.remove(key);

        if (session != null && session.isConnected()) {
            session.disconnect();
            log.info("SSH connection closed for {}", hostname);
        }
    }

    /**
     * Disconnects all SSH sessions.
     */
    public void disconnectAll() {
        log.info("Closing all SSH connections");
        sessionPool.values().forEach(session -> {
            if (session.isConnected()) {
                session.disconnect();
            }
        });
        sessionPool.clear();
    }

    private String createKey(String hostname, int port, String username) {
        return String.format("%s:%d:%s", hostname, port, username);
    }
}
