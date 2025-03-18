/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.commons.util;

/**
 * Class with utility methods for constructing SQL strings.
 *
 * @author Lars Helge Overland
 */
public class SqlHelper {
  private final boolean includeSpaces;

  private boolean whereAndInvoked = false;

  private boolean havingAndInvoked = false;

  private boolean orInvoked = false;

  private boolean betweenAndInvoked = false;

  private boolean andOrInvoked = false;

  private boolean andInvoked = false;

  /** Constructor. */
  public SqlHelper() {
    this.includeSpaces = false;
  }

  /**
   * Constructor.
   *
   * @param includeSpaces whether to prepend and append spaces.
   */
  public SqlHelper(boolean includeSpaces) {
    this.includeSpaces = includeSpaces;
  }

  /**
   * Returns "where" the first time it is invoked, then "and" for subsequent invocations.
   *
   * @return "where" or "and".
   */
  public String whereAnd() {
    String str = whereAndInvoked ? "and" : "where";
    whereAndInvoked = true;
    return padded(str);
  }

  /**
   * Returns "having" the first time it is invoked, then "and" for subsequent invocations.
   *
   * @return "having" or "and".
   */
  public String havingAnd() {
    String str = havingAndInvoked ? "and" : "having";
    havingAndInvoked = true;
    return padded(str);
  }

  /**
   * Returns "between" the first time it is invoked, then "and" for subsequent invocations.
   *
   * @return "between" or "and".
   */
  public String betweenAnd() {
    String str = betweenAndInvoked ? "and" : "between";
    betweenAndInvoked = true;
    return padded(str);
  }

  /**
   * Returns "and" the first time it is invoked, then "or" for subsequent invocations.
   *
   * @return "and" or "or".
   */
  public String andOr() {
    String str = andOrInvoked ? "or" : "and";
    andOrInvoked = true;
    return padded(str);
  }

  /**
   * Returns the empty string the first time it is invoked, then "and" for subsequent invocations.
   *
   * @return empty string or "and".
   */
  public String and() {
    String str = andInvoked ? "and" : "";
    andInvoked = true;
    return padded(str);
  }

  /**
   * Returns the empty string the first time it is invoked, then "or" for subsequent invocations.
   *
   * @return empty string or "or".
   */
  public String or() {
    String str = orInvoked ? "or" : "";
    orInvoked = true;
    return padded(str);
  }

  /**
   * Adds a space to the beginning and end of the given string if the include spaces parameter is
   * true.
   *
   * @param str the string to pad.
   * @return a string.
   */
  private String padded(String str) {
    return includeSpaces ? " " + str + " " : str;
  }
}
