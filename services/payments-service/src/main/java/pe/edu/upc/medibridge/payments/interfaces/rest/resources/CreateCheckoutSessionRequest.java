package pe.edu.upc.medibridge.payments.interfaces.rest.resources;

import pe.edu.upc.medibridge.payments.domain.model.valueobjects.BillingCycle;
import pe.edu.upc.medibridge.payments.domain.model.valueobjects.CommercialLine;
import pe.edu.upc.medibridge.payments.domain.model.valueobjects.PlanType;

public record CreateCheckoutSessionRequest(
        Long userId,
        CommercialLine commercialLine,
        PlanType planType,
        BillingCycle billingCycle) {
}
