/*
 * Copyright (c) 2004-2024, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.user;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.hisp.dhis.common.auth.RegistrationParams;
import org.hisp.dhis.common.auth.UserInviteParams;
import org.hisp.dhis.common.auth.UserRegistrationParams;
import org.hisp.dhis.feedback.BadRequestException;

/**
 * Service that handles user account activities, e.g. create/update account. The Validation can be
 * moved somewhere more appropriate when this is revisited.
 *
 * @author david mackessy
 */
public interface UserAccountService {

  /**
   * Create a self registered user using some self reg defaults (org unit and user role)
   *
   * @param params used to populate the new User
   * @param request used in the authentication process
   */
  void registerUser(UserRegistrationParams params, HttpServletRequest request);

  /**
   * Create an invited user using the restore flow
   *
   * @param params used to populate the updated User
   * @param request used in the authentication process
   */
  void confirmUserInvite(UserInviteParams params, HttpServletRequest request)
      throws BadRequestException;

  /**
   * Validates a self registered user. Uses specific validation for the self registration flow.
   *
   * @param params to validate
   * @param remoteIpAddress address for recaptcha checking
   * @throws BadRequestException when validation error
   * @throws IOException possible when validating recaptcha
   */
  void validateUserRegistration(RegistrationParams params, String remoteIpAddress)
      throws BadRequestException, IOException;

  /**
   * Validates an invited registered user. Uses specific validation for the invite registration
   * flow.
   *
   * @param params to validate
   * @param remoteIpAddress address for recaptcha checking
   * @throws BadRequestException when validation error
   * @throws IOException possible when validating recaptcha
   */
  void validateInvitedUser(RegistrationParams params, String remoteIpAddress)
      throws BadRequestException, IOException;
}
