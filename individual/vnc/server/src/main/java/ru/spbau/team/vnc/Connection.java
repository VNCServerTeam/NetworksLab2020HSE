package ru.spbau.team.vnc;

import ru.spbau.team.vnc.messages.incoming.ClientInitMessage;
import ru.spbau.team.vnc.messages.incoming.SecuritySelectMessage;
import ru.spbau.team.vnc.messages.incoming.VersionSelectMessage;
import ru.spbau.team.vnc.messages.incoming.routine.RoutineMessage;
import ru.spbau.team.vnc.messages.outcoming.*;
import ru.spbau.team.vnc.security.SecurityType;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;

public class Connection {

    private final Socket socket;
    private final Server server;
    private final Parameters parameters;

    public Connection(Socket socket, Server server, Parameters parameters) {
        this.socket = socket;
        this.server = server;
        this.parameters = parameters;
    }

    public void run() {
        try {
            initConnection();
            routine();
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        }
    }

    private void initConnection() throws IOException {
        var selectedVersion = protocolVersionHandshake();
        securityHandshake(selectedVersion);
        initialization();
    }

    private void routine() throws IOException {
        while (true) {
            var routineMessage = RoutineMessage.fromInputStream(socket.getInputStream());
            // TODO
        }
    }

    private VersionSelectMessage protocolVersionHandshake() throws IOException {
        sendMessage(new ProtocolVersionMessage(Server.MAJOR_VERSION, Server.MINOR_VERSION));
        return VersionSelectMessage.fromInputStream(socket.getInputStream());
    }

    private void securityHandshake(VersionSelectMessage selectedVersion) throws IOException {
        if (versionIsNotSupported(selectedVersion)) {
            sendMessage(new SecurityTypesMessage(Collections.emptyList()));
            String errorMessage = "Version " + selectedVersion.getMajorVersion() + "." + selectedVersion.getMinorVersion() + " is not supported";
            sendMessage(new SecurityFailureMessage(errorMessage));
            // TODO throw
        } else {
            sendMessage(new SecurityTypesMessage(Arrays.asList(SecurityType.INVALID, SecurityType.NONE)));
            var security = SecuritySelectMessage.fromInputStream(socket.getInputStream());
            // TODO: use security
            if (security.getSecurityType().equals(SecurityType.INVALID)) {
                sendMessage(new SecurityResultMessage(false));
                sendMessage(new SecurityFailureMessage("Invalid security code"));
            } else  {
                sendMessage(new SecurityResultMessage(true));
            }
        }
    }

    private void initialization() throws IOException {
        var isShared = ClientInitMessage.fromInputStream(socket.getInputStream()).isShared();
        if (isShared) {
            // TODO: disconnect others
        }
        sendMessage(new ServerInitMessage(parameters));
    }

    private boolean versionIsNotSupported(VersionSelectMessage selectedVersion) {
        // TODO: support 3.3, 3.7, 3.x
        return selectedVersion.getMajorVersion() != 3 || selectedVersion.getMinorVersion() != 8;
    }

    private void sendMessage(OutcomingMessage message) throws IOException {
        socket.getOutputStream().write(message.toByteArray());
    }

    void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
