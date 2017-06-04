package org.eliee.cordovaudpbrdcst;
import java.util.Collection;
import java.util.LinkedHashMap;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginEntry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.cordova.splashscreen.SplashScreen;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.eliee.cordovaudpbrdcst.*;

/**
 * This class echoes a string called from JavaScript.
 */
public class BroadcastManager extends CordovaPlugin {

	private static final String TAG             = "broadcastmanager";
	private static final String SEND_BROADCAST  = "send";
	private static final String LISTEN          = "listen";
	private static final String INTERRUPT       = "stopsend";
	private static final String STOP_LISTEN     = "deaf";

	private CallbackContext connectionCallbackContext;
	private boolean listening = false;
	private boolean sending   = false;
	private CordovaInterface cordova;
	private CordovaWebView webView;
	private Context context;
	//will care for all posts
	private Handler mHandler = new Handler();
	private Thread listener;
	private Thread sender;
	
	private BroadcastSender bSender;
	private BroadcastServer bServer;

	//will launch the activity
	/*private Runnable mLaunchTask = new Runnable() {
        public void run() {
            Intent it = new Intent("com.cellip.show.transfer");
            it.setComponent(new ComponentName(context.getPackageName(), MainActivity.class.getName()));
            it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            it.setAction(Intent.ACTION_MAIN);
            it.addCategory(Intent.CATEGORY_LAUNCHER);
            //context.startActivity(it);
            //context.getApplicationContext().startActivity(it);
        }
     };*/

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

		PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
		result.setKeepCallback(true);
		this.connectionCallbackContext = callbackContext;
		this.connectionCallbackContext.sendPluginResult(result);

		String type = null;
		String additional = null;
		
		switch (action) {
		case SEND_BROADCAST:
			if (!sending) {
				bSender = new BroadcastSender(this);
				sending = true;
				sender = new Thread(bSender);
				sender.start();
			}
			type = "server";
			additional = ""+sending;
			break;
		case INTERRUPT:
			if (sending) {
				bSender.stop();
				try {
					sender.join(2500);
				} catch (InterruptedException e) {
					sender.interrupt();
				}
				sending = false;
			}
			type = "server";
			additional = ""+sending;
			break;
		case LISTEN: 
			if (!listening) {
				bServer = new BroadcastServer(this);
				listening = true;
				listener = new Thread(bServer);
				listener.start();
			}
			type = "send";
			additional = ""+listening;
			break;
		case STOP_LISTEN:
			bServer.stop();
			try {
				listener.join(2500);
			} catch (InterruptedException e) {
				listener.interrupt();
				listener = null;
			}
			listening = false;
			type = "send";
			additional = ""+listening;
			break;
		}

		this.sendUpdate(type, additional);
		
		return true;
	}

	private void echo(String message, CallbackContext callbackContext) {

		if (message != null && message.length() > 0) {
			callbackContext.success(message);
		} else {
			callbackContext.error("Expected one non-empty string argument.");
		}
	}

	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		this.connectionCallbackContext = null;
		this.cordova = cordova;
		this.webView = webView;
	}

	protected void sendUpdate(String type, String additional) {
		JSONObject obj = new JSONObject();
		obj.put("type", type);
		obj.put("state", additional);

		if (connectionCallbackContext != null) {
			PluginResult result = new PluginResult(PluginResult.Status.OK, type);
			result.setKeepCallback(true);
			connectionCallbackContext.sendPluginResult(result);
		} else {
		}
	}
}
