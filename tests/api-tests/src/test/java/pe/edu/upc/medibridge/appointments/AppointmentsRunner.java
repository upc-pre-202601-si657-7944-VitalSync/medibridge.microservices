package pe.edu.upc.medibridge.appointments;

import com.intuit.karate.junit5.Karate;

public class AppointmentsRunner {

    @Karate.Test
    Karate testAppointments() {
        return Karate.run("appointments").relativeTo(getClass());
    }
}
