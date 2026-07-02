package pe.edu.upc.medibridge.communication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "pe.edu.upc.medibridge.communication.infrastructure.acl")
public class CommunicationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommunicationServiceApplication.class, args);
    }
}

