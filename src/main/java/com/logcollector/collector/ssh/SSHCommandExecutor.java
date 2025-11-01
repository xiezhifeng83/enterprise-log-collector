package com.logcollector.collector.ssh;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes SSH commands on remote servers.
 *
 * Specialized for executing tail commands for log collection.
 */
@Component
@Slf4j
public class SSHCommandExecutor {

    /**
     * Executes a command on the remote server and returns output.
     *
     * @param session connected SSH session
     * @param command command to execute
     * @return command output lines
     * @throws JSchException if SSH execution fails
     * @throws IOException if reading output fails
     */
    public List<String> executeCommand(Session session, String command) throws JSchException, IOException {
        log.debug("Executing SSH command: {}", command);

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);

        List<String> output = new ArrayList<>();

        try (InputStream in = channel.getInputStream()) {
            channel.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
            }

            // Wait for command to complete
            while (!channel.isClosed()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Command execution interrupted", e);
                }
            }

            int exitCode = channel.getExitStatus();
            if (exitCode != 0) {
                log.warn("SSH command exited with code {}: {}", exitCode, command);
            }

        } finally {
            channel.disconnect();
        }

        log.debug("SSH command completed, returned {} lines", output.size());
        return output;
    }

    /**
     * Executes tail command to read log file from specific position.
     *
     * @param session connected SSH session
     * @param logFilePath path to log file
     * @param startLine line number to start from (1-based)
     * @param maxLines maximum lines to read
     * @return log lines
     * @throws JSchException if SSH execution fails
     * @throws IOException if reading output fails
     */
    public List<String> tailLogFile(Session session, String logFilePath, long startLine, int maxLines)
            throws JSchException, IOException {

        // Build tail command: tail -n +startLine logFilePath | head -n maxLines
        String command;
        if (startLine > 1) {
            command = String.format("tail -n +%d '%s' | head -n %d", startLine, logFilePath, maxLines);
        } else {
            command = String.format("head -n %d '%s'", maxLines, logFilePath);
        }

        return executeCommand(session, command);
    }

    /**
     * Gets the line count of a file.
     *
     * @param session connected SSH session
     * @param logFilePath path to log file
     * @return number of lines in file
     * @throws JSchException if SSH execution fails
     * @throws IOException if reading output fails
     */
    public long getLineCount(Session session, String logFilePath) throws JSchException, IOException {
        String command = String.format("wc -l < '%s'", logFilePath);
        List<String> output = executeCommand(session, command);

        if (output.isEmpty()) {
            return 0;
        }

        try {
            return Long.parseLong(output.get(0).trim());
        } catch (NumberFormatException e) {
            log.warn("Could not parse line count from: {}", output.get(0));
            return 0;
        }
    }
}
