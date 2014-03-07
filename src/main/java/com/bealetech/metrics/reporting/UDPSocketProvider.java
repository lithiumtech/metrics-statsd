package com.bealetech.metrics.reporting;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public interface UDPSocketProvider {
    DatagramSocket get() throws Exception;
    DatagramPacket newPacket(ByteArrayOutputStream out);
}
