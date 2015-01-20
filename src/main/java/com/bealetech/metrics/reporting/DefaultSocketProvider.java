package com.bealetech.metrics.reporting;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class DefaultSocketProvider implements UDPSocketProvider {

    private final String host;
    private final int port;

    public DefaultSocketProvider(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public DatagramSocket get() throws Exception {
        return new DatagramSocket();
    }

    @Override
    public DatagramPacket newPacket(ByteArrayOutputStream out) {
        try {
            if (out != null) {
                return newPacket(out.toByteArray());
            } else {
                return newPacket(new byte[8192]);
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
     public DatagramPacket newPacket(byte[] out) throws UnknownHostException {
        return new DatagramPacket(out, out.length, InetAddress.getByName(this.host), this.port);
    }
}
