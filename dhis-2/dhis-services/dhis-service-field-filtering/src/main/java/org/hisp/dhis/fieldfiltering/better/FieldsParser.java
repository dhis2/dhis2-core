/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.fieldfiltering.better;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;

public class FieldsParser {

  public static Fields parse(String input) {
    // TODO error handling: white space in a field name, special characters like * or : in a field
    // name, things like ', ,   ' or 'group[ ]' or a block without a name '[]'
    FieldsAccumulator root = new FieldsAccumulator();
    Stack<FieldsAccumulator> stack = new Stack<>();
    stack.push(root);

    int i = 0;
    int fieldStart = i;
    boolean inField = false;
    // TODO(ivo) inline this into parseField
    boolean isFieldWithWhitespace = false;
    boolean isExclusion = false;
    while (i < input.length()) {
      if ((input.charAt(i) == ',')) {
        if (inField) {
          if (isExclusion) {
            stack.peek().excludes(parseField(input, fieldStart, i, isFieldWithWhitespace));
          } else {
            stack.peek().includes(parseField(input, fieldStart, i, isFieldWithWhitespace));
          }

          inField = false;
          isFieldWithWhitespace = false;
          isExclusion = false;
        }
      } else if (input.charAt(i) == '[' || input.charAt(i) == '(') {
        // TODO fail if we are not inField? old parser allows this but the result makes no sense
        if (inField) {
          String parent = parseField(input, fieldStart, i, isFieldWithWhitespace);
          if (isExclusion) {
            stack.peek().excludes(parent);
          } else {
            stack.peek().includes(parent);
          }

          stack.push(stack.peek().getOrCreateChild(parent));
          inField = false;
          isFieldWithWhitespace = false;
          isExclusion = false;
        }
      } else if (input.charAt(i) == ']' || input.charAt(i) == ')') {
        if (stack.size() <= 1) {
          throw new IllegalArgumentException("Unbalanced parens/brackets in input: " + input);
        }

        if (inField) {
          if (isExclusion) {
            stack.peek().excludes(parseField(input, fieldStart, i, isFieldWithWhitespace));
          } else {
            stack.peek().includes(parseField(input, fieldStart, i, isFieldWithWhitespace));
          }
        }

        stack.pop();
        inField = false;
        isFieldWithWhitespace = false;
        isExclusion = false;
      } else if (input.charAt(i) == '!' && !inField) {
        inField = true;
        isExclusion = true;
        fieldStart = i + 1; // do not includes ! in field name
      } else if (Character.isWhitespace(input.charAt(i)) && inField) {
        isFieldWithWhitespace = true;
      } else if (!Character.isWhitespace(input.charAt(i)) && !inField) {
        inField = true;
        isFieldWithWhitespace = false;
        isExclusion = false;
        fieldStart = i;
      }
      i++;
    }

    if (inField) {
      if (isExclusion) {
        stack.peek().excludes(parseField(input, fieldStart, i, isFieldWithWhitespace));
      } else {
        stack.peek().includes(parseField(input, fieldStart, i, isFieldWithWhitespace));
      }
    }
    // TODO this is where we could check if stack size is > 1 and err as a bracket/paren
    // "group[name" was not closed

    return map(root, root.includes.contains("*"));
  }

  /**
   * The current {@code FieldFilterParser} has this behavior. We try to avoid building a string for
   * every field name as most will not have any whitespace inside of a field name. Ideally we would
   * not support this and only ignore leading and trailing whitespace.
   */
  private static String parseField(String input, int start, int end, boolean hasWhitespace) {
    String field = input.substring(start, end);

    if (!hasWhitespace) {
      return field;
    }

    StringBuilder sb = new StringBuilder(field.length());
    for (int j = 0; j < field.length(); j++) {
      char c = field.charAt(j);
      if (!Character.isWhitespace(c)) {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private static Fields map(FieldsAccumulator acc, boolean includesAll) {
    Map<String, Fields> children = new HashMap<>();
    for (Map.Entry<String, FieldsAccumulator> entry : acc.children.entrySet()) {
      // Inclusion rules
      // 1. An * from a parent propagates to its children
      // 2. All of a fields children are automatically included unless an explicit inclusion or
      // exclusion is given
      // TODO(ivo) this last rule is not true (at least for all) metadata. The behavior is schema
      // dependent. References like fields=program will turn into fields=program[id]. There is more
      // logic with regards to "complex" objects ... We can come up with a mechanism to override
      // this behavior with a Function that either gets the field or the full path if necessary.
      // This function can then be passed into the parser and depend on the schema service.
      boolean includeChildren =
          includesAll
              || entry.getValue().includes.contains("*")
              || entry
                  .getValue()
                  .includes
                  .isEmpty(); // TODO(ivo) is this one correct? what if it has an exclude only?
      children.put(entry.getKey(), map(entry.getValue(), includeChildren));
    }

    // TODO(ivo) * should not be part of the final fields/children
    Set<String> fields;
    Function<String, Fields> childrenFunc;

    if (includesAll) {
      fields = acc.excludes;
      if (children.isEmpty()) {
        childrenFunc = Fields.ALL_CHILDREN;
      } else {
        childrenFunc =
            (field) -> {
              if (children.containsKey(field)) { // explicit field specification takes precedence
                return children.get(field);
              }
              return Fields
                  .ALL; // since all of the parents fields are included all of the children are as
              // well
            };
      }
    } else {
      fields = new HashSet<>(acc.includes);
      fields.removeAll(acc.excludes);
      // 2. rule from above
      fields.forEach(f -> children.putIfAbsent(f, Fields.ALL));
      childrenFunc = children::get;
    }

    return new Fields(includesAll, fields, childrenFunc, Map.of());
  }

  /**
   * Accumulates included and excluded field names as well as presets and * in a tree like structure
   * representing the (nested) fields expressions.
   */
  private static final class FieldsAccumulator {
    final Set<String> includes = new HashSet<>();
    final Set<String> excludes = new HashSet<>();
    final Map<String, FieldsAccumulator> children = new HashMap<>();

    void includes(String field) {
      this.includes.add(field);
    }

    void excludes(String field) {
      this.excludes.add(field);
    }

    FieldsAccumulator getOrCreateChild(String field) {
      return children.computeIfAbsent(field, k -> new FieldsAccumulator());
    }
  }
}
