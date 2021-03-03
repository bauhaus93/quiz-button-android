package com.example.quizbutton;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class QuizServer implements Runnable {

    private long lastActivation = new Date().getTime() - Constants.TIME_BETWEEN_ACTIVATION;
    int currPlacement = 0;
    Selector selector;
    ServerSocketChannel serverSocket;
    volatile boolean stop =  false;

    public void stop() {
        stop = true;
    }

    @Override
    public void run() {
        Log.i("QUIZ", "Starting quiz server");
        try {
            setupServer();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        while (!stop) {
            try {
                selector.select(250);
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (SelectionKey key: selector.selectedKeys()) {
                if (key.isAcceptable()) {
                    handleAccept();
                } else if(key.isReadable()) {
                    handleReadKey(key);
                }
            }
            selector.selectedKeys().clear();
        }
        closeChannels();
        Log.i("QUIZ", "Stopped server thread");
    }

    private void setupServer() throws IOException {
        selector = Selector.open();
        serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress("0.0.0.0", Constants.HOST_PORT));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
    }

    private void closeChannels() {
        for(SelectionKey key: selector.keys()) {
            try {
                key.channel().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean activationPossible() {
        return lastActivation + Constants.TIME_BETWEEN_ACTIVATION < new Date().getTime();
    }

    private void handleAccept() {
        try {
            SocketChannel client = serverSocket.accept();
                client.configureBlocking(false);
                client.register(selector, SelectionKey.OP_READ);
                Log.i("QUIZ", "Register new client");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleReadKey(SelectionKey key) {
        SocketChannel client = (SocketChannel)key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1);
        Log.i("QUIZ", "Got key for read");
        try {
            if (client.read(buffer) == -1) {
                client.close();
            }
            if (buffer.get(0) == 1){
                buffer.clear();
                if (activationPossible()) {
                    lastActivation = new Date().getTime();
                    currPlacement = 0;
                }
                Log.i("QUIZ", "ACTIVATE!");
                currPlacement++;
                buffer.put(0, (byte)currPlacement);
                client.write(buffer);
            }
        } catch (IOException e) {
            key.cancel();
            Log.i("QUIZ", "Cancel key");
        }
    }
}