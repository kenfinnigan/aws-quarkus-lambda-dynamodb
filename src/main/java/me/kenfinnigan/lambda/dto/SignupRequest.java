package me.kenfinnigan.lambda.dto;

public class SignupRequest {
  private String email;

  private String deviceId;

  public String getEmail() {
    return email;
  }

  public SignupRequest setEmail(String email) {
    this.email = email;
    return this;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public SignupRequest setDeviceId(String deviceId) {
    this.deviceId = deviceId;
    return this;
  }

}
