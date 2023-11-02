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
package org.hisp.dhis.feedback;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@ToString
@Getter
@Setter
@Accessors(chain = true)
public class ErrorReport {

  private final ErrorMessage message;
  @JsonProperty private final Class<?> mainKlass;
  @JsonProperty private String mainId;
  @JsonProperty private Class<?> errorKlass;
  @JsonProperty private String errorProperty;
  @Nonnull @JsonProperty private List<?> errorProperties;
  @JsonProperty private Object value;

  public ErrorReport(Class<?> mainKlass, ErrorCode errorCode, Object... args) {
    this.mainKlass = mainKlass;
    this.message = new ErrorMessage(errorCode, args);
    this.errorProperties = List.of(args);
  }

  public ErrorReport(Class<?> mainKlass, ErrorMessage message) {
    this.mainKlass = mainKlass;
    this.message = message;
    this.errorProperties = message.getArgs();
  }

  @JsonCreator
  public ErrorReport(
      @JsonProperty("message") String message,
      @CheckForNull @JsonProperty("args") List<String> args,
      @JsonProperty("mainKlass") Class<?> mainKlass,
      @JsonProperty("errorCode") ErrorCode errorCode) {
    this.mainKlass = mainKlass;
    this.message = new ErrorMessage(message, errorCode, args);
    this.errorProperties = args == null ? List.of() : args;
  }

  @JsonProperty
  public ErrorCode getErrorCode() {
    return message.getErrorCode();
  }

  @JsonProperty
  public String getMessage() {
    return message.getMessage();
  }

  @JsonProperty
  public List<String> getArgs() {
    return message.getArgs();
  }
}
