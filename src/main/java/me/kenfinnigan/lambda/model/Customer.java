package me.kenfinnigan.lambda.model;

import java.time.Instant;
import java.util.Objects;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@DynamoDbBean
public class Customer {
  public static final String CUSTOMER_TABLE_NAME = "customers";
  public static final String EMAIL_INDEX = "customer_email_index";
  public static final String TOKEN_INDEX = "customer_token_index";
  public static final String PARTITION_KEY = "customer_id";

  private String customerId;
  private String email;
  private String deviceId;
  private String token;
  private Instant createdAt;
  private Instant updatedAt;

  public Customer() {
  }

  @DynamoDbPartitionKey
  @DynamoDbAttribute(PARTITION_KEY)
  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String id) {
    this.customerId = id;
  }

  @DynamoDbSecondaryPartitionKey(indexNames = EMAIL_INDEX)
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @DynamoDbAttribute("device_id")
  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  @DynamoDbSecondaryPartitionKey(indexNames = TOKEN_INDEX)
  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof Customer)) {
      return false;
    }

    Customer other = (Customer) obj;

    return Objects.equals(other, obj);
  }

  @Override
  public int hashCode() {
    return Objects.hash(customerId, email, deviceId, createdAt);
  }
}
