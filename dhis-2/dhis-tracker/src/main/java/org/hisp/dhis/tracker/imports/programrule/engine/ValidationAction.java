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
package org.hisp.dhis.tracker.imports.programrule.engine;

import java.util.Arrays;

public enum ValidationAction {
  ASSIGN("ASSIGN"),
  SET_MANDATORY_FIELD("SETMANDATORYFIELD"),
  SHOW_ERROR("SHOWERROR"),
  SHOW_WARNING("SHOWWARNING"),
  SHOW_ERROR_ON_COMPLETE("ERRORONCOMPLETE"),
  SHOW_WARNING_ON_COMPLETE("WARNINGONCOMPLETE"),
  RAISE_ERROR("ERROR"),
  SCHEDULE_EVENT("SCHEDULEEVENT");

  public static boolean contains(String ruleEngineName) {
    return Arrays.stream(values()).anyMatch(v -> v.ruleEngineName.equalsIgnoreCase(ruleEngineName));
  }

  public static ValidationAction fromName(String ruleEngineName) {
    return Arrays.stream(values())
        .filter(v -> v.ruleEngineName.equalsIgnoreCase(ruleEngineName))
        .findAny()
        .orElseThrow();
  }

  private final String ruleEngineName;

  ValidationAction(String ruleEngineName) {
    this.ruleEngineName = ruleEngineName;
  }
}
