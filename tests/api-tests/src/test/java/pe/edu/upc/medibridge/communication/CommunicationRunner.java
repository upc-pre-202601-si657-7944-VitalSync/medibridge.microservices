package pe.edu.upc.medibridge.communication;

import com.intuit.karate.junit5.Karate;

public class CommunicationRunner {

    @Karate.Test
    Karate testCommunication() {
        return Karate.run("communication").relativeTo(getClass());
    }
}
