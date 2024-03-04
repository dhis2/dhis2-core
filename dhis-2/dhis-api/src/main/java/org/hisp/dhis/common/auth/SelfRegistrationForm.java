package org.hisp.dhis.common.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SelfRegistrationForm {
  @JsonProperty String username;
  @JsonProperty String firstName;
  @JsonProperty String surname;
  @JsonProperty String password;
  @JsonProperty String email;
  @JsonProperty String phoneNumber;

  @JsonProperty("g-recaptcha-response")
  String recaptchaResponse;
}
