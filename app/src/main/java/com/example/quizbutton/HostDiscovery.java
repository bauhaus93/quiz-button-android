package com.example.quizbutton;

import android.util.Log;

import org.apache.commons.net.util.SubnetUtils;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class HostDiscovery implements Runnable {

    Socket foundHostSocket;
    final short targetPort;
    final AtomicBoolean foundHost = new AtomicBoolean(false);
    final AtomicReference<ConnectionState> result = new AtomicReference<>(ConnectionState.NOT_CONNECTED);

    public HostDiscovery(short targetPort) {
        this.targetPort = targetPort;
    }

    public ConnectionState getState() {
        return result.get();
    }
    public synchronized Socket getHost() {
        return foundHostSocket;
    }

    @Override
    public synchronized void run() {
        result.set(ConnectionState.HOST_DISCOVERY);
        SubnetUtils.SubnetInfo subnetInfo = getWifiSubnet();
        if (subnetInfo == null) {
            result.set(ConnectionState.NO_WIFI_FOUND);
            return;
        }
        Log.i("QUIZ", "subnet:"  + subnetInfo.getCidrSignature());
        String[] addresses = subnetInfo.getAllAddresses();
        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<Callable<Socket>> tasks = new ArrayList<>();
        for(String addr: addresses) {
            tasks.add(() -> {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(addr, targetPort), Constants.HOST_DISCOVERY_CONNECTION_TIMEOUT);
                if (socket.isConnected()) {
                    return socket;
                }
                throw new Exception("Socket not connected");
            });
        }
        try {
            foundHostSocket = executor.invokeAny(tasks);
            result.set(ConnectionState.CONNECTED);
        } catch (ExecutionException e) {
            result.set(ConnectionState.NO_HOST_FOUND);
            Log.d("QUIZ", "No task could be executed successfully");
        } catch (InterruptedException e) {
            result.set(ConnectionState.NO_HOST_FOUND);
            Log.d("QUIZ", "Execution was interrupted");
        }
    }

    private SubnetUtils.SubnetInfo getWifiSubnet() {
        try {
            for (final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements(); ) {
                final NetworkInterface curr = interfaces.nextElement();
                if (!curr.getName().contains("wlan")) {
                    continue;
                }

                Log.i("QUIZ", "interface: " + curr.getName());
                for (final InterfaceAddress addr : curr.getInterfaceAddresses()) {
                    if (addr.getAddress() instanceof Inet4Address) {
                        final Inet4Address inetAddr = (Inet4Address) addr.getAddress();
                        String cidr = String.format("%s/%d", inetAddr.toString().substring(1), addr.getNetworkPrefixLength());
                        SubnetUtils subnet = new SubnetUtils(cidr);
                        return subnet.getInfo();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }
}
