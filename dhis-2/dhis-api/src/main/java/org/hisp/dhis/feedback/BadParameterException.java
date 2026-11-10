/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.feedback;

import java.text.MessageFormat;
import javax.annotation.Nonnull;
import lombok.Getter;

/**
 * Unfortunately Java records do not allow to throw checked exceptions. To still use parameter
 * records and prevent bad parameters by construction we need a {@link RuntimeException} that they
 * can throw which then can transform to a checked exception.
 *
 * <p>This is specifically for issues that are client errors, in other words that are {@link
 * BadRequestException}s. For example, values out of range, missing required values, combinations of
 * parameters that do not work together.
 *
 * @author Jan Bernitt
 */
@Getter
public final class BadParameterException extends IllegalArgumentException {

  @Nonnull private final ErrorCode code;
  @Nonnull private final Object[] args;

  public BadParameterException(@Nonnull ErrorCode code, @Nonnull Object... args) {
    super(MessageFormat.format(code.getMessage(), args));
    this.code = code;
    this.args = args;
  }

  public BadRequestException toChecked() {
    return new BadRequestException(code, args);
  }
}
