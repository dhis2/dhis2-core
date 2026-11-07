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
package org.hisp.dhis.tracker.imports.programrule.engine;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Everything that can be used to write a program rule expression (conditions and program rule
 * action data field expressions share the same syntax). Clients like the program rule editor use it
 * to offer auto-completion and to document the expression language.
 *
 * <p>This is DHIS2's own representation of the syntax exposed by the rule engine, so that the rule
 * engine types do not leak into the API.
 *
 * @param functions the functions, e.g. {@code d2:daysBetween}
 * @param binaryOperators the binary operators in order of precedence (highest first), e.g. {@code
 *     &&}
 * @param unaryOperators the unary operators, e.g. {@code !}
 * @param environmentVariables the environment variables provided by the rule engine, referenced as
 *     {@code V{name}}
 */
public record ProgramRuleExpressionSyntax(
    @JsonProperty @Nonnull List<Function> functions,
    @JsonProperty @Nonnull List<Operator> binaryOperators,
    @JsonProperty @Nonnull List<Operator> unaryOperators,
    @JsonProperty @Nonnull List<EnvironmentVariable> environmentVariables) {

  public ProgramRuleExpressionSyntax {
    functions = List.copyOf(functions);
    binaryOperators = List.copyOf(binaryOperators);
    unaryOperators = List.copyOf(unaryOperators);
    environmentVariables = List.copyOf(environmentVariables);
  }

  /**
   * A function that can be used in an expression.
   *
   * @param name the name of the function as written in an expression, e.g. {@code d2:daysBetween}
   * @param returnType the type of the value returned by the function
   * @param parameters the parameters in the order they are accepted
   */
  public record Function(
      @JsonProperty @Nonnull String name,
      @JsonProperty @Nonnull ValueType returnType,
      @JsonProperty @Nonnull List<Parameter> parameters) {
    public Function {
      parameters = List.copyOf(parameters);
    }
  }

  /**
   * A parameter of a {@link Function}.
   *
   * @param name a descriptive name of the parameter (e.g. {@code regex}), or null if the meaning is
   *     obvious from the function name (e.g. the only parameter of {@code d2:ceil})
   * @param type the type expected for the argument
   * @param optional true, if the argument can be omitted (optional parameters are always last)
   * @param varargs true, if this is the last parameter and it can be repeated
   */
  public record Parameter(
      @JsonProperty @JsonInclude(JsonInclude.Include.NON_NULL) @CheckForNull String name,
      @JsonProperty @Nonnull ValueType type,
      @JsonProperty boolean optional,
      @JsonProperty boolean varargs) {}

  /**
   * An operator that can be used in an expression.
   *
   * @param symbol the canonical symbol of the operator, e.g. {@code &&}
   * @param aliases alternative spellings of the operator that are also accepted, e.g. {@code and}
   * @param returnType the type of the value returned by the operator
   * @param operandsType the type expected for the operand(s)
   */
  public record Operator(
      @JsonProperty @Nonnull String symbol,
      @JsonProperty @Nonnull List<String> aliases,
      @JsonProperty @Nonnull ValueType returnType,
      @JsonProperty @Nonnull ValueType operandsType) {
    public Operator {
      aliases = List.copyOf(aliases);
    }
  }

  /**
   * An environment variable provided by the rule engine.
   *
   * @param name the name of the variable as referenced in {@code V{name}}, e.g. {@code
   *     current_date}
   * @param valueType the type of the value of the variable
   */
  public record EnvironmentVariable(
      @JsonProperty @Nonnull String name, @JsonProperty @Nonnull ValueType valueType) {}

  /** The type of a value accepted or returned by a function, operator or variable. */
  public enum ValueType {
    /** The value can be of more than one type, or the type cannot be determined statically. */
    MIXED,
    NUMBER,
    BOOLEAN,
    DATE,
    STRING,
    /**
     * The value can be of any type, but all values typed as {@code SAME} in a signature must have
     * the same actual type. As a return type, the result has the actual type of the {@code SAME}
     * parameter(s).
     */
    SAME
  }
}
