package com.lithium.dog.event;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.DatagramSocket;
import java.net.UnknownHostException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DataDogEventClientTest  {

    private DatagramSocket socket;

    @Before
    public void setUp() throws Exception {
        socket = new DatagramSocket();
    }

    @After
    public void tearDown() throws Exception {
        if (socket != null) {
            socket.close();
        }
    }

    @Test
    public void testSendEvent() throws Exception {
        final ErrorHandler errHandler = mock(ErrorHandler.class);
        final DataDogEventClient client = new DataDogEventClient("localhost",
                                                                 socket.getLocalPort(),
                                                                 Collections.singleton("always:tagged"),
                                                                 errHandler);
        final EventMessage msgSent = client.sendEvent("foo", "baar", "baz:biz");
        assertEquals("_e{3,4}:foo|baar|#baz:biz,always:tagged", msgSent.toString());
        verify(errHandler, never()).handle(any(UnknownHostException.class));
    }

    @Test
    public void testSendEvent_InvalidHost() throws Exception {
        final ErrorHandler errHandler = mock(ErrorHandler.class);
        final DataDogEventClient client = new DataDogEventClient("not.a.valid.hostname",
                                                                 8125,
                                                                 Collections.singleton("always:tagged"),
                                                                 errHandler);
        final EventMessage msgSent = client.sendEvent("foo", "bar");
        assertNull(msgSent);
        verify(errHandler, times(1)).handle(any(UnknownHostException.class));
    }

    @Test
    public void testSendEvent_NoConstantTags() throws Exception {
        final ErrorHandler errHandler = mock(ErrorHandler.class);
        final DataDogEventClient client = new DataDogEventClient("localhost",
                                                                 socket.getLocalPort(),
                                                                 null /* constant tags*/,
                                                                 errHandler);
        final EventMessage msgSent = client.sendEvent("foo", "baar", "baz:biz");
        assertEquals("_e{3,4}:foo|baar|#baz:biz", msgSent.toString());
        verify(errHandler, never()).handle(any(UnknownHostException.class));
    }

    @Test
    public void testSendEvent_NoEventTags() throws Exception {
        final ErrorHandler errHandler = mock(ErrorHandler.class);
        final DataDogEventClient client = new DataDogEventClient("localhost",
                                                                 socket.getLocalPort(),
                                                                 Collections.singleton("always:tagged"),
                                                                 errHandler);
        final EventMessage msgSent = client.sendEvent("foo", "baar");
        assertEquals("_e{3,4}:foo|baar|#always:tagged", msgSent.toString());
        verify(errHandler, never()).handle(any(UnknownHostException.class));
    }


}
