package com.example.quizbutton;

import android.util.Log;

import org.apache.commons.net.util.SubnetUtils;

import java.io.IOException;
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
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class HostDiscovery implements Runnable {

    Socket foundHostSocket;
    short targetPort;
    AtomicBoolean foundHost = new AtomicBoolean(false);
    AtomicBoolean discoveryActive = new AtomicBoolean(false);
    AtomicReference<String> result = new AtomicReference<>("Host discovery...");

    public HostDiscovery(short targetPort) {
        this.targetPort = targetPort;
    }

    public boolean foundHost() {
        return foundHost.get();
    }
    public boolean isActive() {
        return discoveryActive.get();
    }
    public String getResultText() {
        return result.get();
    }
    public synchronized Socket getHost() {
        return foundHostSocket;
    }

    @Override
    public synchronized void run() {
        discoveryActive.set(true);
        SubnetUtils.SubnetInfo subnetInfo = getWifiSubnet();
        if (subnetInfo == null) {
            result.set("No wifi found");
            discoveryActive.set(false);
            return;
        }
        Log.i("QUIZ", "subnet:"  + subnetInfo.getCidrSignature());
        String[] addresses = subnetInfo.getAllAddresses();
        ExecutorService executor = Executors.newFixedThreadPool(10);
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
            result.set("Connected");
            foundHost.set(true);
        } catch (ExecutionException e) {
            result.set("No host found");
            Log.e("QUIZ", "ExecutionException while ExecutorService.invokeAny");
        } catch (InterruptedException e) {
            result.set("No host found");
            Log.e("QUIZ", "InterruptedException while ExecutorService.invokeAny");
        }
        discoveryActive.set(false);
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
