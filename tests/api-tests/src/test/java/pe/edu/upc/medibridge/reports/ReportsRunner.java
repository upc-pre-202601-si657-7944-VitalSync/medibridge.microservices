package pe.edu.upc.medibridge.reports;

import com.intuit.karate.junit5.Karate;

public class ReportsRunner {

    @Karate.Test
    Karate testReports() {
        return Karate.run("reports").relativeTo(getClass());
    }
}
