package me.kenfinnigan.lambda.dto;

public class SignupResponse {
  private String customerId;
  private String customerToken;

  public String getCustomerId() {
    return customerId;
  }

  public SignupResponse setCustomerId(String customerId) {
    this.customerId = customerId;
    return this;
  }

  public String getCustomerToken() {
    return customerToken;
  }

  public SignupResponse setCustomerToken(String customerToken) {
    this.customerToken = customerToken;
    return this;
  }

}
