package com.example.quizbutton;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;

public class QuizServer implements Runnable {

    private static QuizServer instance;
    private long lastActivation = new Date().getTime() - Constants.TIME_BETWEEN_ACTIVATION;
    int currPlacement = 0;
    Selector selector;
    ServerSocketChannel serverSocket;
    volatile boolean stop =  false;

    private QuizServer() {
    }

    public static QuizServer getInstance() {
        if (QuizServer.instance == null) {
            Log.d("QUIZ", "Creating new QuizServer singleton");
            QuizServer.instance = new QuizServer();
        }
        return QuizServer.instance;
    }

    public static void start() {
        Thread thread = new Thread(QuizServer.getInstance());
        thread.start();
    }

    public static void destroy() {
        if (QuizServer.instance != null) {
            Log.d("QUIZ", "Destroying QuizServer singleton");
            QuizServer.instance.stop();
            QuizServer.instance = null;
        }
    }

    public void stop() {
        stop = true;
    }

    @Override
    public void run() {
        Log.d("QUIZ", "Starting quiz server");
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
        Log.d("QUIZ", "Stopped server thread");
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
        try {
            selector.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
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
                Log.d("QUIZ", "Register new client:" + client.getRemoteAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleReadKey(SelectionKey key) {
        SocketChannel client = (SocketChannel)key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1);
        Log.d("QUIZ", "Got key for read");
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
                Log.d("QUIZ", "Activation invoked");
                currPlacement++;
                buffer.put(0, (byte)currPlacement);
                client.write(buffer);
            }
        } catch (IOException e) {
            key.cancel();
            Log.d("QUIZ", "Cancel key");
        }
    }
}