package pe.edu.upc.medibridge.payments;

import com.intuit.karate.junit5.Karate;

public class PaymentsRunner {

    @Karate.Test
    Karate testPayments() {
        return Karate.run("payments").relativeTo(getClass());
    }
}
