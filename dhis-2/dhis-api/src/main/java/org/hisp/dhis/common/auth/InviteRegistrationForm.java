package org.hisp.dhis.common.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class InviteRegistrationForm extends SelfRegistrationForm {
  @JsonProperty String inviteUsername;
  @JsonProperty String inviteToken;
}
