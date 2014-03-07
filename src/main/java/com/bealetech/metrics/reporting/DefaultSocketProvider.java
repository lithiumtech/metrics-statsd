package com.bealetech.metrics.reporting;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
* Created by matthew.bogner on 3/6/14.
*/
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
        byte[] dataBuffer;

        if (out != null) {
            dataBuffer = out.toByteArray();
        }
        else {
            dataBuffer = new byte[8192];
        }

        try {
            return new DatagramPacket(dataBuffer, dataBuffer.length, InetAddress.getByName(this.host), this.port);
        } catch (Exception e) {
            return null;
        }
    }
}
