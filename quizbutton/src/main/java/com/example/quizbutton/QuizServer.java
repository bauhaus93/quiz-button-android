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
    volatile boolean stop = false;

    private QuizServer() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public static QuizServer getInstance() {
        if (QuizServer.instance == null) {
            Log.d("QUIZ", "Creating new QuizServer singleton");
            QuizServer.instance = new QuizServer();
        }
        return QuizServer.instance;
    }

    public static boolean isActive() {
        return QuizServer.instance != null;
    }

    public static boolean hasStopped() {
        return QuizServer.instance != null && QuizServer.instance.stop;
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
            Log.e("QUIZ", "Couldn't setup server");
            return;
        }
        while (!stop) {
            try {
                selector.select(1000);
            } catch (IOException e) {
                Log.e("QUIZ" ,"Server error on selection");
                stop = true;
                break;
            }
            for (SelectionKey key: selector.selectedKeys()) {
                try {
                    if (key.isAcceptable()) {
                        handleAccept();
                    } else if (key.isReadable()) {
                        handleReadKey(key);
                    }
                } catch (IOException e) {
                    Log.e("QUIZ", "Couldn't handle selected key, closing channel");
                    try {
                        key.channel().close();
                    } catch (IOException ioException) {
                        Log.e("QUIZ", "Couldn't close channel of key after error");
                    }
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
        if (selector != null) {
            for(SelectionKey key: selector.keys()) {
                try {
                    key.channel().close();
                } catch (IOException e) {
                    Log.e("QUIZ", "Couldn't close channel of key on termination");
                }
            }
        }
        try {
            if (selector != null) {
                selector.close();
            }
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Log.e("QUIZ", "Couldn't close selector/serverSocket on termination");
        }
    }

    private boolean activationPossible() {
        return lastActivation + Constants.TIME_BETWEEN_ACTIVATION < new Date().getTime();
    }

    private void handleAccept() throws IOException {
        SocketChannel client = serverSocket.accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
            Log.d("QUIZ", "Register new client: " + client.getRemoteAddress().toString().substring(1));
    }

    private void handleReadKey(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel)key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1);
        Log.d("QUIZ", "Got key for read");
        if (client.read(buffer) == -1) {
            Log.d("QUIZ", "Client closed connection");
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
    }
}