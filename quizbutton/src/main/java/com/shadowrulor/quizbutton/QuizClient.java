package com.shadowrulor.quizbutton;

import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class QuizClient implements Runnable  {

    private Socket socket;
    private long lastActivation = new Date().getTime() - Constants.TIME_BETWEEN_ACTIVATION;
    private boolean stop = false;
    private final AtomicInteger placement = new AtomicInteger(0);

    public QuizClient(Socket host) {
        socket = host;
    }

    public synchronized void stop() {
        stop = true;
        super.notify();
    }

    public synchronized boolean sendButtonPressed() {
        if (activationPossible()) {
            placement.set(0);
            super.notify();
            return true;
        }
        return false;
    }

    public int getPlacement() {
        return placement.get();
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    private boolean activationPossible() {
        return lastActivation + Constants.TIME_BETWEEN_ACTIVATION < new Date().getTime();
    }

    @Override
    public synchronized void run() {
        while (!stop) {
            try {
                super.wait();
                if (!stop) {
                    cycle();
                }
            } catch (IOException e) {
                Log.d("QUIZ", "Client got disconnected");
                socket = null;
                stop = true;
            } catch (InterruptedException e) {
                Log.e("QUIZ", "Client loop got interrupted");
            }
        }
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            Log.e("QUIZ", "Client couldn't close socket on termination");
        }
        Log.d("QUIZ", "Stopped client thread");
    }

    private synchronized void cycle() throws IOException  {
        socket.getOutputStream().write((byte) 1);
        byte[] msg = new byte[]{0};
        socket.getInputStream().read(msg);
        placement.set(msg[0]);
        lastActivation = new Date().getTime();
    }

}
