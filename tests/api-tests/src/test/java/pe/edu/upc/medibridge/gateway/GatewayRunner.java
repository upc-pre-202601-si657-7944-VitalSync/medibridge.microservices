package pe.edu.upc.medibridge.gateway;

import com.intuit.karate.junit5.Karate;

public class GatewayRunner {

    @Karate.Test
    Karate testGateway() {
        return Karate.run("gateway").relativeTo(getClass());
    }
}
