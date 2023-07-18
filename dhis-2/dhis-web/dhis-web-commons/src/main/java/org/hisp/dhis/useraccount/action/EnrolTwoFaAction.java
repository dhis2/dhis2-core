/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.useraccount.action;

import com.opensymphony.xwork2.Action;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.security.TwoFactoryAuthenticationUtils;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class EnrolTwoFaAction implements Action {
  private SystemSettingManager systemSettingManager;

  private UserService userService;

  @Autowired
  public void setSystemSettingManager(SystemSettingManager systemSettingManager) {
    this.systemSettingManager = systemSettingManager;
  }

  @Autowired
  public void setUserService(UserService userService) {
    this.userService = userService;
  }

  private String username;

  private String image;

  public String getUsername() {
    return username;
  }

  public String getImage() {
    return image;
  }

  @Override
  public String execute() throws Exception {
    this.username =
        (String) ServletActionContext.getRequest().getSession().getAttribute("username");

    User user = userService.getUserByUsername(username);

    if (userService.hasTwoFactorRoleRestriction(user)
        && (!user.isTwoFactorEnabled() || UserService.hasTwoFactorSecretForApproval(user))) {
      userService.generateTwoFactorOtpSecretForApproval(user);

      List<ErrorCode> errorCodes = new ArrayList<>();

      this.image = generateQrCode(user, errorCodes);

      if (errorCodes.isEmpty()) {
        return SUCCESS;
      }
    }

    return ERROR;
  }

  private String generateQrCode(User user, List<ErrorCode> errorCodes) {
    String appName = systemSettingManager.getStringSetting(SettingKey.APPLICATION_TITLE);

    String qrContent =
        TwoFactoryAuthenticationUtils.generateQrContent(appName, user, errorCodes::add);

    byte[] qrCode =
        TwoFactoryAuthenticationUtils.generateQRCode(qrContent, 200, 200, errorCodes::add);

    return "data:image/png;base64," + Base64.getEncoder().encodeToString(qrCode);
  }
}
