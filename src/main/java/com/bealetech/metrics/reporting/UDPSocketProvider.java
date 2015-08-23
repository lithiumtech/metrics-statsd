package com.bealetech.metrics.reporting;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.UnknownHostException;

public interface UDPSocketProvider {
    DatagramSocket get() throws Exception;
    DatagramPacket newPacket(ByteArrayOutputStream out);
    DatagramPacket newPacket(byte[] out) throws UnknownHostException;
}
