package me.kenfinnigan.lambda;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent.RequestContext;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent.RequestContext.Http;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.kenfinnigan.lambda.dto.SignupRequest;
import me.kenfinnigan.lambda.dto.SignupResponse;
import me.kenfinnigan.lambda.model.Customer;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

@QuarkusTest
@TestInstance(Lifecycle.PER_CLASS)
public class CreateCustomerTest {
    @Inject
    DynamoDbClient dynamoDbClient;

    @Inject
    ObjectMapper objectMapper;

    Map<String, String> getAttributes() {
        return Map.of(
                Customer.PARTITION_KEY, "S",
                "email", "S",
                "token", "S");
    }

    GlobalSecondaryIndex[] getGlobalSecondaryIndexes() {
        return Arrays.asList(
                getEmailIndex(),
                getTokenIndex()).toArray(new GlobalSecondaryIndex[2]);
    }

    GlobalSecondaryIndex getEmailIndex() {
        return GlobalSecondaryIndex.builder()
                .indexName(Customer.EMAIL_INDEX)
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName("email")
                                .keyType("HASH")
                                .build())
                .projection(p -> p.projectionType(ProjectionType.ALL))
                .build();
    }

    GlobalSecondaryIndex getTokenIndex() {
        return GlobalSecondaryIndex.builder()
                .indexName(Customer.TOKEN_INDEX)
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName("token")
                                .keyType("HASH")
                                .build())
                .projection(p -> p.projectionType(ProjectionType.ALL))
                .build();
    }

    @BeforeAll
    void setup() {
        if (dynamoDbClient.listTables().tableNames().contains(Customer.CUSTOMER_TABLE_NAME)) {
            return;
        }

        CreateTableRequest.Builder createTableRequestBuilder = CreateTableRequest.builder()
                .tableName(Customer.CUSTOMER_TABLE_NAME)
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName(Customer.PARTITION_KEY)
                                .keyType("HASH")
                                .build())
                .billingMode("PAY_PER_REQUEST");

        Collection<AttributeDefinition> attributeDefinitions = getAttributes().entrySet().stream()
                .map(e -> AttributeDefinition.builder()
                        .attributeName(e.getKey())
                        .attributeType(e.getValue())
                        .build())
                .toList();
        createTableRequestBuilder.attributeDefinitions(attributeDefinitions);

        if (getGlobalSecondaryIndexes() != null) {
            createTableRequestBuilder.globalSecondaryIndexes(getGlobalSecondaryIndexes());
        }

        dynamoDbClient.createTable(createTableRequestBuilder.build());
    }

    @Test
    void success() throws Exception {
        SignupRequest body = new SignupRequest();
        body.setEmail("gary.sinise@gmail.com");
        body.setDeviceId("1234");

        APIGatewayV2HTTPEvent request = APIGatewayV2HTTPEvent.builder()
                .withRequestContext(
                        RequestContext.builder()
                                .withHttp(
                                        Http.builder()
                                                .withMethod("POST")
                                                .build())
                                .build())
                .withBody(objectMapper.writeValueAsString(body))
                .build();

        Response response = given()
                .contentType("application/json")
                .accept("application/json")
                .body(request)
                .when()
                .post()
                .thenReturn();

        // Assert on response to APIGateway
        assertTrue(null != response);
        assertEquals(200, response.getStatusCode());

        // Assert on response to client
        APIGatewayV2HTTPResponse out = response.getBody().as(APIGatewayV2HTTPResponse.class);
        assertTrue(null != out);
        assertEquals(200, out.getStatusCode());
        SignupResponse signupResponse = objectMapper.readValue(out.getBody(), SignupResponse.class);
        assertTrue(null != signupResponse);
        assertTrue(null != signupResponse.getCustomerId());
        assertTrue(null != signupResponse.getCustomerToken());
    }

    @Test
    void failWithMissingData() throws Exception {
        APIGatewayV2HTTPEvent request = APIGatewayV2HTTPEvent.builder()
                .withRequestContext(
                        RequestContext.builder()
                                .withHttp(
                                        Http.builder()
                                                .withMethod("POST")
                                                .build())
                                .build())
                .withBody(objectMapper.writeValueAsString(new SignupRequest()))
                .build();

        Response response = given()
                .contentType("application/json")
                .accept("application/json")
                .body(request)
                .when()
                .post()
                .thenReturn();

        // Assert on response to APIGateway
        assertTrue(null != response);
        assertEquals(200, response.getStatusCode());

        // Assert on response to client
        APIGatewayV2HTTPResponse out = response.getBody().as(APIGatewayV2HTTPResponse.class);
        assertTrue(null != out);
        assertEquals(400, out.getStatusCode());
    }

    @Test
    void failWithMissingEmail() throws Exception {
        APIGatewayV2HTTPEvent request = APIGatewayV2HTTPEvent.builder()
                .withRequestContext(
                        RequestContext.builder()
                                .withHttp(
                                        Http.builder()
                                                .withMethod("POST")
                                                .build())
                                .build())
                .withBody(objectMapper.writeValueAsString(new SignupRequest().setDeviceId("1234")))
                .build();

        Response response = given()
                .contentType("application/json")
                .accept("application/json")
                .body(request)
                .when()
                .post()
                .thenReturn();

        // Assert on response to APIGateway
        assertTrue(null != response);
        assertEquals(200, response.getStatusCode());

        // Assert on response to client
        APIGatewayV2HTTPResponse out = response.getBody().as(APIGatewayV2HTTPResponse.class);
        assertTrue(null != out);
        assertEquals(400, out.getStatusCode());
    }

    @Test
    void failWithMissingDeviceId() throws Exception {
        APIGatewayV2HTTPEvent request = APIGatewayV2HTTPEvent.builder()
                .withRequestContext(
                        RequestContext.builder()
                                .withHttp(
                                        Http.builder()
                                                .withMethod("POST")
                                                .build())
                                .build())
                .withBody(objectMapper.writeValueAsString(new SignupRequest().setEmail("gary.cole@gmail.com")))
                .build();

        Response response = given()
                .contentType("application/json")
                .accept("application/json")
                .body(request)
                .when()
                .post()
                .thenReturn();

        // Assert on response to APIGateway
        assertTrue(null != response);
        assertEquals(200, response.getStatusCode());

        // Assert on response to client
        APIGatewayV2HTTPResponse out = response.getBody().as(APIGatewayV2HTTPResponse.class);
        assertTrue(null != out);
        assertEquals(400, out.getStatusCode());
    }

    @Test
    void failWithInvalidEmail() throws Exception {
        SignupRequest body = new SignupRequest();
        body.setEmail("gary.sinise.gmail.com");
        body.setDeviceId("1234");

        APIGatewayV2HTTPEvent request = APIGatewayV2HTTPEvent.builder()
                .withRequestContext(
                        RequestContext.builder()
                                .withHttp(
                                        Http.builder()
                                                .withMethod("POST")
                                                .build())
                                .build())
                .withBody(objectMapper.writeValueAsString(body))
                .build();

        Response response = given()
                .contentType("application/json")
                .accept("application/json")
                .body(request)
                .when()
                .post()
                .thenReturn();

        // Assert on response to APIGateway
        assertTrue(null != response);
        assertEquals(200, response.getStatusCode());

        // Assert on response to client
        APIGatewayV2HTTPResponse out = response.getBody().as(APIGatewayV2HTTPResponse.class);
        assertTrue(null != out);
        assertEquals(400, out.getStatusCode());
    }
}
