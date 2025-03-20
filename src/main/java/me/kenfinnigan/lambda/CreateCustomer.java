package me.kenfinnigan.lambda;

import java.time.Instant;
import java.util.stream.Stream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.amazon.dynamodb.enhanced.runtime.NamedDynamoDbTable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import me.kenfinnigan.lambda.dto.SignupRequest;
import me.kenfinnigan.lambda.dto.SignupResponse;
import me.kenfinnigan.lambda.model.Customer;
import me.kenfinnigan.lambda.util.EmailUtil;
import me.kenfinnigan.lambda.util.TokenUtil;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Named("createCustomer")
public class CreateCustomer implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
  @Inject
  ObjectMapper objectMapper;

  @Inject
  @NamedDynamoDbTable(Customer.CUSTOMER_TABLE_NAME)
  DynamoDbTable<Customer> customerTable;

  @Override
  public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent requestEvent, Context context) {
    if (!requestEvent.getRequestContext().getHttp().getMethod().equals("POST")) {
      return APIGatewayV2HTTPResponse.builder()
          .withStatusCode(405)
          .withBody("Method Not Allowed")
          .build();
    }

    try {
      String body = requestEvent.getBody();
      if (null == body || body.isEmpty()) {
        return APIGatewayV2HTTPResponse.builder()
            .withStatusCode(400)
            .withBody("Invalid request")
            .build();
      }

      SignupRequest request = objectMapper.readValue(body, SignupRequest.class);

      // Check input parameters
      if (null == request.getEmail() || request.getEmail().isEmpty()) {
        return APIGatewayV2HTTPResponse.builder()
            .withStatusCode(400)
            .withBody("Invalid request")
            .build();
      }
      if (null == request.getDeviceId() || request.getDeviceId().isEmpty()) {
        return APIGatewayV2HTTPResponse.builder()
            .withStatusCode(400)
            .withBody("Invalid request")
            .build();
      }

      // Check for valid email
      if (!EmailUtil.isValidEmail(request.getEmail())) {
        return APIGatewayV2HTTPResponse.builder()
            .withStatusCode(400)
            .withBody("Invalid request")
            .build();
      }
      // Check DB for unique email
      Customer existingCustomer = getCustomerByEmail(request.getEmail());
      if (null != existingCustomer) {
        return APIGatewayV2HTTPResponse.builder()
            .withStatusCode(409)
            .withBody("Account already exists")
            .build();
      }

      // Generate a unique customer ID
      String customerId = TokenUtil.generateCustomerId();

      // Generate a unique customer token
      String token = TokenUtil.generateDeviceToken(customerId, request.getDeviceId());

      // Create customer
      Customer customer = new Customer();
      customer.setCustomerId(customerId);
      customer.setEmail(request.getEmail());
      customer.setDeviceId(request.getDeviceId());
      customer.setToken(token);
      customer = createCustomer(customer);

      SignupResponse response = new SignupResponse();
      response.setCustomerId(customer.getCustomerId());
      response.setCustomerToken(customer.getToken());

      return APIGatewayV2HTTPResponse.builder()
          .withStatusCode(200)
          .withBody(objectMapper.writeValueAsString(response))
          .build();
    } catch (IllegalArgumentException iae) {
      return APIGatewayV2HTTPResponse.builder()
          .withStatusCode(400)
          .withBody(iae.getMessage())
          .build();
    } catch (Exception e) {
      return APIGatewayV2HTTPResponse.builder()
          .withStatusCode(500)
          .withBody(e.getMessage())
          .build();
    }
  }

  public Customer createCustomer(Customer customer) {
    if (null == customer.getCustomerId()) {
      customer.setCustomerId(TokenUtil.generateCustomerId());
    }

    if (null == customer.getCreatedAt()) {
      Instant now = Instant.now();
      customer.setCreatedAt(now);
      customer.setUpdatedAt(now);
    }

    customerTable.putItem(customer);
    return customer;
  }

  public Customer getCustomerByEmail(String email) {
    QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(email).build());
    return getItemFromStream(customerTable.index(Customer.EMAIL_INDEX).query(queryConditional).stream());
  }

  private <T> T getItemFromStream(Stream<Page<T>> items) {
    Page<T> pageResult = items.findFirst().orElse(null);

    if (null != pageResult) {
      if (pageResult.count() > 1) {
        throw new IllegalStateException("Expected only one item");
      } else if (pageResult.count() == 0) {
        return null;
      }
    }

    return pageResult.items().get(0);
  }

}
