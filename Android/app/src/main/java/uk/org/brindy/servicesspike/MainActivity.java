package uk.org.brindy.servicesspike;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class MainActivity extends AppCompatActivity implements NsdManager.RegistrationListener {

    private NsdManager nsdManager;
    private ServerSocket serverSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            startServer();
            registerService();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        nsdManager.unregisterService(this);

        try {
            stopServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
        Log.d(getClass().getSimpleName(), "onRegistrationFailed, " + errorCode);
    }

    @Override
    public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
        Log.d(getClass().getSimpleName(), "onUnregistrationFailed, " + errorCode);
    }

    @Override
    public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
        Log.d(getClass().getSimpleName(), "onServiceRegistered");
    }

    @Override
    public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
        Log.d(getClass().getSimpleName(), "onServiceUnregistered");
    }

    private void startServer() throws IOException {
        serverSocket = new ServerSocket(0);
        serverSocket.setSoTimeout(10000);
        new SpikeServer(serverSocket).start();
    }

    private void stopServer() throws IOException {
        serverSocket.close();
    }

    private void registerService() {

        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setServiceName("Spike (" + serverSocket.getLocalPort() + ")");
        serviceInfo.setServiceType("_spike._tcp");
        serviceInfo.setPort(serverSocket.getLocalPort());

        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, this);
    }

    static class SpikeServer extends Thread {

        private final ServerSocket serverSocket;

        public SpikeServer(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        @Override
        public void run() {
            Log.d("SpikeServer", "run new server socket" + serverSocket);

            while(true) {
                try {
                    Log.d("SpikeServer", "waiting for connection");
                    new SpikeSocket(serverSocket.accept()).start();
                } catch (SocketTimeoutException ex) {
                    Log.d("SpikeServer", "timeout while waiting for connection");
                    // expected
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    static class SpikeSocket extends Thread {

        private final Socket socket;

        public SpikeSocket(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            Log.d("SpikeSocket", "run new socket " + socket);
            try {
                _run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void _run() throws IOException {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            byte[] header = new byte[3];
            if (3 != in.read(header)) {
                throw new IOException("Invalid Spike Header");
            }
            Log.d("SpikeSocket", toString(header));

            out.write(new byte[] { 1, 2, 3 });
        }

        private String toString(byte[] bytes) {
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append("0x").append(Integer.toHexString(b)).append(" ");
            }
            return builder.toString().trim().replace(" ", ", ");
        }
    }

}
