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
package org.hisp.dhis.security.vote;

import com.opensymphony.xwork2.config.entities.ActionConfig;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.security.StrutsAuthorityUtils;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author Torgeir Lorange Ostby
 * @version $Id: ActionAccessVoter.java 6335 2008-11-20 11:11:26Z larshelg $
 */
@Slf4j
public class ActionAccessVoter extends AbstractPrefixedAccessDecisionVoter {

  // -------------------------------------------------------------------------
  // AccessDecisionVoter Input
  // -------------------------------------------------------------------------

  private String requiredAuthoritiesKey;

  public void setRequiredAuthoritiesKey(String requiredAuthoritiesKey) {
    this.requiredAuthoritiesKey = requiredAuthoritiesKey;
  }

  private String anyAuthoritiesKey;

  public void setAnyAuthoritiesKey(String anyAuthoritiesKey) {
    this.anyAuthoritiesKey = anyAuthoritiesKey;
  }

  // -------------------------------------------------------------------------
  // AccessDecisionVoter implementation
  // -------------------------------------------------------------------------

  @Override
  public boolean supports(Class<?> clazz) {
    boolean result = ActionConfig.class.equals(clazz);

    log.debug("Supports class: " + clazz + ", " + result);

    return result;
  }

  @Override
  public int vote(
      Authentication authentication, Object object, Collection<ConfigAttribute> attributes) {
    if (!supports(object.getClass())) {
      log.debug("ACCESS_ABSTAIN [" + object.toString() + "]: Class not supported.");

      return AccessDecisionVoter.ACCESS_ABSTAIN;
    }

    ActionConfig actionConfig = (ActionConfig) object;
    Collection<ConfigAttribute> requiredAuthorities =
        StrutsAuthorityUtils.getConfigAttributes(actionConfig, requiredAuthoritiesKey);
    Collection<ConfigAttribute> anyAuthorities =
        StrutsAuthorityUtils.getConfigAttributes(actionConfig, anyAuthoritiesKey);

    int allStatus = allAuthorities(authentication, object, requiredAuthorities);

    if (allStatus == AccessDecisionVoter.ACCESS_DENIED) {
      return AccessDecisionVoter.ACCESS_DENIED;
    }

    int anyStatus = anyAuthority(authentication, object, anyAuthorities);

    if (anyStatus == AccessDecisionVoter.ACCESS_DENIED) {
      return AccessDecisionVoter.ACCESS_DENIED;
    }

    if (allStatus == AccessDecisionVoter.ACCESS_GRANTED
        || anyStatus == AccessDecisionVoter.ACCESS_GRANTED) {
      return AccessDecisionVoter.ACCESS_GRANTED;
    }

    return AccessDecisionVoter.ACCESS_ABSTAIN;
  }

  private int allAuthorities(
      Authentication authentication, Object object, Collection<ConfigAttribute> attributes) {
    int supported = 0;

    for (ConfigAttribute attribute : attributes) {
      if (supports(attribute)) {
        ++supported;
        boolean found = false;

        for (GrantedAuthority authority : authentication.getAuthorities()) {
          if (authority.getAuthority().equals(attribute.getAttribute())) {
            found = true;
            break;
          }
        }

        if (!found) {
          log.debug("ACCESS_DENIED [" + object.toString() + "]");

          return AccessDecisionVoter.ACCESS_DENIED;
        }
      }
    }

    if (supported > 0) {
      log.debug("ACCESS_GRANTED [" + object.toString() + "]");

      return AccessDecisionVoter.ACCESS_GRANTED;
    }

    log.debug("ACCESS_ABSTAIN [" + object.toString() + "]: No supported attributes.");

    return AccessDecisionVoter.ACCESS_ABSTAIN;
  }

  private int anyAuthority(
      Authentication authentication, Object object, Collection<ConfigAttribute> attributes) {
    int supported = 0;
    boolean found = false;

    for (ConfigAttribute attribute : attributes) {
      if (supports(attribute)) {
        ++supported;

        for (GrantedAuthority authority : authentication.getAuthorities()) {
          if (authority.getAuthority().equals(attribute.getAttribute())) {
            found = true;
            break;
          }
        }
      }
    }

    if (!found && supported > 0) {
      log.debug("ACCESS_DENIED [" + object.toString() + "]");

      return AccessDecisionVoter.ACCESS_DENIED;
    }

    if (supported > 0) {
      log.debug("ACCESS_GRANTED [" + object.toString() + "]");

      return AccessDecisionVoter.ACCESS_GRANTED;
    }

    log.debug("ACCESS_ABSTAIN [" + object.toString() + "]: No supported attributes.");

    return AccessDecisionVoter.ACCESS_ABSTAIN;
  }
}
