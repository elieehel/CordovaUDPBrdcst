package org.eliee.cordovaudpbrdcst;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.eliee.cordovaudpbrdcst.*;

import android.util.Log;

public class BroadcastServer implements Runnable {
	
	DatagramSocket socket;
    private static final String TAG = "brdcstserver";
    private boolean run = true;
    
    private BroadcastManager bM;
    
    public BroadcastServer(BroadcastManager bM) {
    	this.bM = bM;
	}
    
    public void stop() {
    	this.run = false;
    	try {
    		socket.close();
    	} catch (IOException e) {
    		
    	}
    }
	
	public void run() {
        try {
            //Keep a socket open to listen to all the UDP trafic that is destined for this port
            socket = new DatagramSocket(1338, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);

            while (this.run && !Thread.currentThread().isInterrupted()) {
                Log.i(TAG,"Ready to receive broadcast packets!");

                //Receive a packet
                byte[] recvBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet);

                //Packet received
                Log.i(TAG, "Packet received from: " + packet.getAddress().getHostAddress());
                String data = new String(packet.getData()).trim();
                Log.i(TAG, "Packet received; data: " + data);
                if (data.equals("issrv")) {
	                byte[] sendData = "ok".getBytes();
	                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
	                socket.send(sendPacket);
                }

            }
        } catch (IOException ex) {
            Log.i(TAG, "Oops" + ex.getMessage());
        }
    }
}