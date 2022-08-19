package uk.co.digitme.machinemonitoring.Helpers;

import android.content.Context;
import android.os.StrictMode;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.Enumeration;

public class ServerDiscovery {

    private static final int DISCOVERY_TIMEOUT_MS = 10000;
    private static final String DISCOVERY_REQUEST_MESSAGE = "DISCOVER_OEE_SERVER_REQUEST";
    private static final String DISCOVERY_RESPONSE_MESSAGE = "DISCOVER_OEE_SERVER_RESPONSE";
    private static final String TAG = "ServerDiscovery";

    public static boolean findServer(Context context) {
        //Find the server using UDP broadcast
        DbHelper dbHelper = new DbHelper(context);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        String ip;


        try {
            //Open a random port to send the package
            DatagramSocket datagramSocket = new DatagramSocket();
            datagramSocket.setBroadcast(true);
            datagramSocket.setSoTimeout(DISCOVERY_TIMEOUT_MS);

            byte[] sendData = DISCOVERY_REQUEST_MESSAGE.getBytes();

            //Broadcast the message all over the network interfaces
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;   //Don't want to broadcast to the loopback interface
                }

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null) {
                        continue;
                    }
                    //Send the broadcast packet
                    try {
                        DatagramPacket sendPacket = new DatagramPacket(
                                sendData,
                                sendData.length,
                                broadcast,
                                8090);
                        datagramSocket.send(sendPacket);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                    Log.v(TAG,
                            "Request packet sent to: " +
                                    broadcast.getHostAddress() +
                                    "; Interface: " +
                                    networkInterface.getDisplayName());
                }
            }

            Log.v(TAG, "Done looping over network interfaces. Waiting for a reply");

            //Wait for a response
            byte[] receiveBuf = new byte[15000];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
            //Show a dialog if the connection times out
            try {
                datagramSocket.receive(receivePacket);
            } catch (SocketTimeoutException e) {

                Log.v(TAG, "Socket timeout. Exiting...");
                datagramSocket.close();
                return false;
            }
            //We have a response
            Log.v(TAG, "Broadcast response from server: " + receivePacket.getAddress().getHostAddress());

            //Check if the message is correct
            String message = new String(receivePacket.getData()).trim();
            if (message.equals(DISCOVERY_RESPONSE_MESSAGE)) {
                ip = receivePacket.getAddress().toString();
                Log.v(TAG, "HOST IP IS " + ip);
                String address = "http://" + ip + ":80";  // todo support other ports
                //Save the ip as a preference
                dbHelper.saveServerAddress(address);
                return true;
            }

            datagramSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return false;

    }

}
