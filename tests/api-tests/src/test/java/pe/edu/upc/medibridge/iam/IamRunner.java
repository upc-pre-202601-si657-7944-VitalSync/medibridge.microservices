package pe.edu.upc.medibridge.iam;

import com.intuit.karate.junit5.Karate;

public class IamRunner {

    @Karate.Test
    Karate testIam() {
        return Karate.run("iam").relativeTo(getClass());
    }
}
