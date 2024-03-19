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
package org.hisp.dhis.query;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.ToString;

/**
 * Simple class for caching of object type. Mainly for usage in speeding up Operator type lookup.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Getter
@ToString
public final class Type {
  private final boolean isString;

  private final boolean isChar;

  private final boolean isByte;

  private final boolean isNumber;

  private final boolean isInteger;

  private final boolean isFloat;

  private final boolean isDouble;

  private final boolean isBoolean;

  private final boolean isEnum;

  private final boolean isDate;

  private final boolean isCollection;

  private final boolean isList;

  private final boolean isSet;

  private final boolean isNull;

  public Type(Object object) {
    isNull = object == null;
    isString = object instanceof String;
    isChar = object instanceof Character;
    isByte = object instanceof Byte;
    isNumber = object instanceof Number;
    isInteger = object instanceof Integer;
    isFloat = object instanceof Float;
    isDouble = object instanceof Double;
    isBoolean = object instanceof Boolean;
    isEnum = object instanceof Enum;
    isDate = object instanceof Date;
    isCollection = object instanceof Collection;
    isList = object instanceof List;
    isSet = object instanceof Set;
  }
}
