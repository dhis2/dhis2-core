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
import java.util.stream.Collectors;

public class FieldsParser {
  /** Fields token that includes all fields. */
  private static final String TOKEN_ALL = "*";

  private static final String TOKEN_PRESET_ALL = ":all";

  public static Fields parse(String input) {
    FieldsAccumulator root = new FieldsAccumulator();
    Stack<FieldsAccumulator> stack = new Stack<>();
    stack.push(root);

    int i = 0;
    int fieldStart = i;
    boolean inField = false;
    boolean isExclusion = false;
    while (i < input.length()) {
      if ((input.charAt(i) == ',')) {
        if (inField) {
          if (isExclusion) {
            stack.peek().excludes(parseField(input, fieldStart, i));
          } else {
            stack.peek().includes(parseField(input, fieldStart, i));
          }

          inField = false;
          isExclusion = false;
        }
      } else if (input.charAt(i) == '[' || input.charAt(i) == '(') {
        if (!inField) { // fields=[value] is ignored, ideally we would reject this
          throw new IllegalArgumentException("Block must have a field name like orgUnits[code]");
        }

        String parent = parseField(input, fieldStart, i);
        if (isExclusion) {
          stack.peek().excludes(parent);
        } else {
          stack.peek().includes(parent);
        }

        stack.push(stack.peek().getOrCreateChild(parent));
        inField = false;
        isExclusion = false;
      } else if (input.charAt(i) == ']' || input.charAt(i) == ')') {
        if (stack.size() == 1) {
          throw new IllegalArgumentException("Unbalanced parens/brackets in input: " + input);
        }

        if (inField) {
          if (isExclusion) {
            stack.peek().excludes(parseField(input, fieldStart, i));
          } else {
            stack.peek().includes(parseField(input, fieldStart, i));
          }
        }

        stack.pop();
        inField = false;
        isExclusion = false;
      } else if (input.charAt(i) == '!' && !inField) {
        inField = true;
        isExclusion = true;
        fieldStart = i + 1; // do not includes ! in field name
      } else if (!Character.isWhitespace(input.charAt(i)) && !inField) {
        inField = true;
        isExclusion = false;
        fieldStart = i;
      }
      i++;
    }

    if (inField) {
      if (isExclusion) {
        stack.peek().excludes(parseField(input, fieldStart, i));
      } else {
        stack.peek().includes(parseField(input, fieldStart, i));
      }
    }
    // this is where we should check if stack size is > 1 and err as a bracket/paren
    // fields="group[name" was not closed

    mapPresets(root);
    return map(root, root.includes.contains(TOKEN_ALL));
  }

  /**
   * The current {@code FieldFilterParser} has this behavior. We check for whitespace in the field
   * and remove it if present. Ideally we would not support this and only ignore leading and
   * trailing whitespace.
   */
  private static String parseField(String input, int start, int end) {
    String field = input.substring(start, end);

    boolean hasWhitespace = false;
    for (int j = 0; j < field.length(); j++) {
      if (Character.isWhitespace(field.charAt(j))) {
        hasWhitespace = true;
        break;
      }
    }

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

  private static void mapPresets(FieldsAccumulator acc) {
    acc.includes =
        acc.includes.stream()
            .map(f -> TOKEN_PRESET_ALL.equals(f) ? TOKEN_ALL : f)
            .collect(Collectors.toSet());

    for (FieldsAccumulator child : acc.children.values()) {
      mapPresets(child);
    }
  }

  /** Maps in depth-first search order each field and its children to {@link Fields}. */
  private static Fields map(FieldsAccumulator acc, boolean includesAll) {
    // fields with `[]` i.e. fields=dataValues[value] will have accumulated children processed here
    Map<String, Fields> children = new HashMap<>();
    for (Map.Entry<String, FieldsAccumulator> entry : acc.children.entrySet()) {
      boolean includeChildren =
          entry.getValue().includes.contains(TOKEN_ALL) // fields=dataValues[*] all are included
              || entry
                  .getValue()
                  .includes
                  .isEmpty(); // fields=dataValues[!value] all but value are included
      children.put(entry.getKey(), map(entry.getValue(), includeChildren));
    }
    acc.includes.remove(TOKEN_ALL);

    Set<String> fields = includesAll ? acc.excludes : new HashSet<>(acc.includes);
    if (!includesAll) {
      // exclusion has precedence over inclusion
      fields.removeAll(acc.excludes);
    }

    return new Fields(includesAll, fields, children, Map.of());
  }

  /**
   * Accumulates included and excluded field names as well as presets and * in a tree like structure
   * representing the (nested) fields expressions.
   */
  private static final class FieldsAccumulator {
    Set<String> includes = new HashSet<>();
    final Set<String> excludes = new HashSet<>();
    final Map<String, FieldsAccumulator> children = new HashMap<>();

    boolean isEmpty() {
      return includes.isEmpty() && excludes.isEmpty();
    }

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
