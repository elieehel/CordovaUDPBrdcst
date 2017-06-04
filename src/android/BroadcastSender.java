package org.eliee.cordovaudpbrdcst;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;

import org.eliee.cordovaudpbrdcst.*;

import android.util.Log;

public class BroadcastSender implements Runnable {
	DatagramSocket socket;
	private static final String TAG = "brdcstlistener";

	private BroadcastManager bM;
	private Context mContext;

	public BroadcastSender(BroadcastManager bM) {
		this.bM = bM;
		this.mContext = bM.getContext();
	}

	public void stop() {
		try {
			socket.close();
		} catch (IOException e) {
			
		}
	}

	public void run() {
		sendBroadcast("issrv");
	}

	private void sendBroadcast(String messageStr) {
		// Hack Prevent crash (sending should be done using an async task)
		StrictMode.ThreadPolicy policy = new   StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		try {
			//Open a random port to send the package
			DatagramSocket socket = new DatagramSocket();
			socket.setBroadcast(true);
			byte[] sendData = messageStr.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, getBroadcastAddress(), 1338);
			socket.send(sendPacket);
			System.out.println(getClass().getName() + "Broadcast packet sent to: " + getBroadcastAddress().getHostAddress());
			byte[] recvBuf = new byte[15000];
			DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
			socket.receive(receivePacket);

			//We have a response
			System.out.println(getClass().getName() + ": Broadcast response from server: " + receivePacket.getAddress().getHostAddress());

			bM.sendUpdate("result", receivePacket.getAddress().getHostAddress());
			socket.close();
		} catch (IOException e) {
			Log.e(TAG, "IOException: " + e.getMessage());
		}
	}

	private InetAddress getBroadcastAddress() throws IOException {
		WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		DhcpInfo dhcp = wifi.getDhcpInfo();
		// handle null somehow

		int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++)
			quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
		return InetAddress.getByAddress(quads);
	}
}