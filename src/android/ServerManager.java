package org.eliee.cordovaudpbrdcst;

import com.cellip.websocket.Command;
import com.cellip.websocket.CommandParser;
import com.cellip.websocket.Server;
import com.cellip.websocket.ServerConfiguration;
import com.cellip.websocket.Util;
import com.cellip.websocket.client.Client;
import com.cellip.websocket.event.EventHandler;
import com.cellip.websocket.event.IEvent;
import com.cellip.websocket.frame.Frame;
import com.cellip.websocket.frame.TextFrame;

import com.cellip.websocket.CommandParser.CommandResult;
import com.cellip.websocket.event.OnEventListener;
import com.cellip.websocket.frame.CloseFrame.CloseCodes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


public class ServerManager {
    private Thread serverThread;
    private Server server;
    private boolean running = false;
	private EventHandler eventHandler;
    public void start() {
        if (running)
            return;
        running = true;
        ServerConfiguration serverConf = ServerConfiguration.init(1337, "Yatzy");
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
        serverConf.addResource("client");
        serverConf.addResource("master");
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
		this.eventHandler = server.getEventHandler();
        
        CommandParser cp = new CommandParser();

        ConcurrentHashMap<String,Client> clients = new ConcurrentHashMap<>();
        
        cp.addCommand("subscribe", new Command("subscribe", "client", "master") {
            @Override
            public Object execute(Client client, Map<String, String> map) {
                clients.put(client.getClientId(), client);
                return null;
            }
        });

        cp.addCommand("new_state", new Command("new_state", "master") {
            @Override
            public Object execute(Client client, Map<String, String> map) {
                for (Client c : clients.values())
                    c.queueData(TextFrame.fragmentedTextframe(map.get("msg")));
                return null;
            }
        });
        
		this.eventHandler.registerListener("on_message", new OnMessageListener(cp));
    }
    
    private class OnMessageListener extends OnEventListener {

    	/**
    	 * Initialize a new OnMessageListener, setting the internal {@link CommandParser}.
    	 * @param cp the CommandParser
    	 */
    	public OnMessageListener(CommandParser cp) {
    		super(cp);
    	}

    	/**
    	 * Receives the event fired when a {@link Client} sends a message to the server.<br><br>
    	 * The message is then treated as a potential command and this listener will try to execute it as such. Should it be a valid command, any results will be returned to the requestor.
    	 * @param e the event fired
    	 */
    	@SuppressWarnings("unchecked")
    	@Override
    	public void onEvent(IEvent e) {
    		Frame incoming = (Frame) e.getParams()[0];
    		Client client = (Client) e.getParams()[1];

    		Util.print("Message: %s %.512s", incoming.getOpcode(), incoming.getPayloadAsString());

    		CommandParser cp = (CommandParser) this.params[0];

    		CommandResult retVal = cp.parseCommand(incoming.getPayloadAsString(),
    				client.getResources(),
    				client
    				);
    		if (retVal.getResultCode() == CommandParser.ResultCodes.SUCCESS) {
    			Util.print("Success returned: %s", retVal.getReturnObject());

    		} else {
    			int reqId = -1;
    			HashMap<String,Object> cparams = (HashMap<String,Object>)retVal.getReturnObject();
    			if (cparams != null && cparams.get("reqId") != null)
    				reqId = Integer.parseInt((String)cparams.get("reqId"));
    			Util.print("Error in command parser! ");
    			Util.print("%s %s", retVal.getReturnObject(), retVal.getResultCode());
    			if (retVal.getResultCode() == CommandParser.ResultCodes.NOT_AVAILABLE_TO_RESOURCE) {
    				return;
    			} else if (retVal.getResultCode() == CommandParser.ResultCodes.FATAL_FAILURE) {
    				client.close(1008, "Fatal violation");
    				return;
    			}
    			Command c = retVal.getCommand();
    			if (c != null) {
    				Util.print("Error message: " + c.getErrorMsg());
    			}
    		}
    	}
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
