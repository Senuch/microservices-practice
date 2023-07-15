package com.jerry.customer;

import com.jerry.amqp.RabbitMQMessageProducer;
import com.jerry.clients.fraud.FraudCheckResponse;
import com.jerry.clients.fraud.FraudClient;
import com.jerry.clients.notification.NotificationRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomerService {
    private final RabbitMQMessageProducer rabbitMQMessageProducer;
    private final CustomerRepository customerRepository;
    private final FraudClient fraudClient;

    public void registerCustomer(CustomerRegistrationRequest request) {
        Customer customer = Customer.builder().firstName(request.firstName()).lastName(request.lastName()).email(request.email()).build();
        customerRepository.saveAndFlush(customer);

        FraudCheckResponse fraudCheckResponse = fraudClient.isFraudster(customer.getId());
        if (fraudCheckResponse.isFraudster()) {
            throw new IllegalStateException("Fraudster");
        }

        NotificationRequest notificationRequest = new NotificationRequest(
                customer.getId(),
                customer.getEmail(),
                String.format("Hi %s, welcome to micro-services...", customer.getFirstName())
        );

        rabbitMQMessageProducer.publish(
                notificationRequest,
                "internal.exchange",
                "internal.notification.routing-key"
        );
    }
}
