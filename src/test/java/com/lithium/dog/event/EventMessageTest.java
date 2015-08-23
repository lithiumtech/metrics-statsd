package com.lithium.dog.event;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class EventMessageTest {

    @Test
    public void testToString() throws Exception {

        EventMessage msg;

        msg = new EventMessage("Foo", "Baaaar", null /* aggregationKey */, null /* priority */, null /* alertType */, null /* tags */);
        assertEquals("_e{3,6}:Foo|Baaaar", msg.toString());

        msg = new EventMessage("Foo", "Baaaar", null /* aggregationKey */, null /* priority */, null /* alertType */, Collections.<String>emptySet());
        assertEquals("_e{3,6}:Foo|Baaaar", msg.toString());

        msg = new EventMessage("Foo", "Baaaar", null /* aggregationKey */, null /* priority */, null /* alertType */, Collections.singleton("baz:biz"));
        assertEquals("_e{3,6}:Foo|Baaaar|#baz:biz", msg.toString());

        msg = new EventMessage("Foo", "Baaaar", null /* aggregationKey */, null /* priority */, null /* alertType */, Arrays.asList("baz:biz", "baz:buz"));
        assertEquals("_e{3,6}:Foo|Baaaar|#baz:biz,baz:buz", msg.toString());

        msg = new EventMessage("Foo", "Baaaar", "aggregationKey", null /* priority */, null /* alertType */, null /* tags */);
        assertEquals("_e{3,6}:Foo|Baaaar|k:aggregationKey", msg.toString());

        msg = new EventMessage("Foo", "Baaaar", null /* aggregationKey */, Priority.low, null /* alertType */, null /* tags */);
        assertEquals("_e{3,6}:Foo|Baaaar|p:low", msg.toString());

        msg = new EventMessage("Foo", "Baaaar", null /* aggregationKey */, null /* priority */, AlertType.success, null /* tags */);
        assertEquals("_e{3,6}:Foo|Baaaar|t:success", msg.toString());

        msg = new EventMessage("Foo", "Baaaar", "aggregationKey", Priority.normal, AlertType.error, Arrays.asList("baz:biz", "baz:buz"));
        assertEquals("_e{3,6}:Foo|Baaaar|k:aggregationKey|p:normal|t:error|#baz:biz,baz:buz", msg.toString());
    }
}
