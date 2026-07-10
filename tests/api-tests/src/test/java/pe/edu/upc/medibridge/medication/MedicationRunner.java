package pe.edu.upc.medibridge.medication;

import com.intuit.karate.junit5.Karate;

public class MedicationRunner {

    @Karate.Test
    Karate testMedication() {
        return Karate.run("medication").relativeTo(getClass());
    }
}
