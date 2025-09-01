/*
 * Copyright (c) 2025 Splatgames.de Software and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.splatgames.aether.generators.mvc.processor.utils;

import org.jetbrains.annotations.Nullable;

/**
 * String utility helpers used by the MVC builder code generator.
 *
 * <p>This class provides naming transformations that are commonly needed
 * when generating fluent APIs (e.g., method names like {@code withX}, or
 * element-level names for collection operations).</p>
 *
 * <p>The class is non-instantiable and only exposes static helper methods.</p>
 *
 * <h2>Typical usage</h2>
 * <blockquote><pre>
 * String alias = GenHelpers.capitalize("role");      // "Role"
 * String singular = GenHelpers.singularize("roles"); // "role"
 * String var = GenHelpers.singularVar("roles");      // "role"
 *
 * // Generated method names:
 * String addMethod = "add" + GenHelpers.capitalize(singular); // "addRole"
 * </pre></blockquote>
 *
 * @author Erik Pförtner
 * @since 1.0.0
 */
public final class GenHelpers {

    /**
     * Private constructor to prevent instantiation.
     */
    private GenHelpers() {
        // utility class; not instantiable
    }

    /**
     * Capitalizes the first character of the given string.
     *
     * @param s the input string (may be {@code null} or empty)
     * @return the string with its first character uppercased,
     * or the original value if {@code null} or empty
     */
    @Nullable
    public static String capitalize(@Nullable final String s) {
        return (s == null || s.isEmpty()) ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Produces a singular form of the given alias.
     *
     * <p>This is a heuristic intended for code generation, not full
     * linguistic correctness. It covers a few common English plural
     * forms and otherwise appends {@code "Item"} as a fallback.</p>
     *
     * @param alias the plural name (may be {@code null} or empty)
     * @return the singular form, or the input if no transformation is possible
     */
    @Nullable
    public static String singularize(@Nullable final String alias) {
        if (alias == null || alias.isEmpty()) return alias;
        if (alias.endsWith("ies") && alias.length() > 3) return alias.substring(0, alias.length() - 3) + "y";
        if (alias.endsWith("ses") && alias.length() > 3) return alias.substring(0, alias.length() - 2);
        if (alias.endsWith("s") && alias.length() > 1) return alias.substring(0, alias.length() - 1);
        return alias + "Item";
    }

    /**
     * Produces a variable-friendly singular name from the given field name.
     *
     * <p>This method combines {@link #singularize(String)} with a lowercase
     * first character, making the result suitable as a local variable or
     * parameter name in generated code.</p>
     *
     * @param fieldName the plural field name (may be {@code null} or empty)
     * @return a lowercased singular form, or the original name if no transformation is possible
     */
    @Nullable
    public static String singularVar(@Nullable final String fieldName) {
        String s = singularize(fieldName);
        if (s == null || s.isEmpty()) return fieldName;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
