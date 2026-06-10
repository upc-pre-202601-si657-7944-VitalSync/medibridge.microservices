package pe.edu.upc.medibridge.profiles;

import com.intuit.karate.junit5.Karate;

class ProfilesRunner {

    @Karate.Test
    Karate testProfiles() {
        return Karate.run("profiles").relativeTo(getClass());
    }
}
