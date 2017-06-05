package org.eliee.cordovaudpbrdcst;

import com.cellip.websocket.Server;
import com.cellip.websocket.ServerConfiguration;
import com.cellip.websocket.Util;

import java.io.IOException;
import java.util.regex.Pattern;


public class ServerManager {
    private Thread serverThread;
    private Server server;
    private boolean running = false;
    public void start() {
        if (running)
            return;
        running = true;
        ServerConfiguration serverConf = ServerConfiguration.init(1337, "Server365");
        // ping message that clients have to be able to interpret - could just as well be a single "p"
        serverConf.setPingMessage("{\"type\":\"ping\"}");
        // what the clients must reply with after receiving a ping message
        serverConf.setPingReply("pong");
        // how often (in server ticks) to ping clients
        serverConf.setPingInterval(45);
        // within how many server ticks must the client reply to the ping message?
        serverConf.setPingTimeout(15);
        // use SSL
        serverConf.setUseSSL(false);
        // whether we check where the client is connecting from or not
        serverConf.setCheckOrigin(false);
        serverConf.setHost(Pattern.compile(".*"));
        // add resources - works a lot like directories in html, e.g. "cellip.com/queries" where "queries" would be a directory
        // access to commands is dependent on which "directory" you connect to
        serverConf.addResource("notifications");
        // number of dispatchamangers to handle incoming and outgoing messages
        serverConf.setDispatchManagers(4);
        serverConf.setPingClients(true);

        // create a new server object
        try {
            Util.DEBUG = true;
            Util.LOGGING = false;
            server = new Server(true);
        } catch (IOException e1) {
            Util.log(Util.defaultLog, "EXCEPTION", "Exception!", e1, null);
            throw new IllegalStateException("Couldn't start the server!");
        }

        //this.serverId = serverConf.getServerId();
        // are we running a reactor server?
        serverConf.setIsReactor(true);
        // define how long a server tick is in ms
        serverConf.setTickTime(1000);

        serverThread = new Thread(server);
        serverThread.start();
        serverThread.setName("Server thread");
    }

    public void stop() {
        try {
            this.server.stopServer();
            this.serverThread.join();
        } catch (InterruptedException e) {
            this.serverThread.interrupt();
        }
        running = false;
    }
}
