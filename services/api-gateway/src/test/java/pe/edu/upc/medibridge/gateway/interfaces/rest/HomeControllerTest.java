package pe.edu.upc.medibridge.gateway.interfaces.rest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class HomeControllerTest {

    @Test
    void redirectToSwaggerReturnsSwaggerUiRedirect() {
        var controller = spy(new HomeController());

        var redirect = controller.redirectToSwagger();

        assertThat(redirect).isEqualTo("redirect:/swagger-ui.html");
        verify(controller).redirectToSwagger();
    }
}
