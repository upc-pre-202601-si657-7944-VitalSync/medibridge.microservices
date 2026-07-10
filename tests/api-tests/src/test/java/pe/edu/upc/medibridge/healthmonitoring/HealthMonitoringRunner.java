package pe.edu.upc.medibridge.healthmonitoring;

import com.intuit.karate.junit5.Karate;

public class HealthMonitoringRunner {

    @Karate.Test
    Karate testHealthMonitoring() {
        return Karate.run("healthmonitoring").relativeTo(getClass());
    }
}
