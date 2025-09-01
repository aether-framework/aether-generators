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

package de.splatgames.aether.generators.mvc.processor.struct;

import com.squareup.javapoet.TypeName;

/**
 * Immutable descriptor of a domain field as seen by the code generator.
 *
 * <p>This record captures all metadata required to emit a fluent builder API and the corresponding
 * object construction logic (including defaults, collections, and relation handling).</p>
 *
 * <h2>Typical usage</h2>
 * <blockquote><pre>
 * // Collected from an entity type during annotation processing:
 * List&lt;FieldModel&gt; fields = collectFields(entityType, includeSuper, explicitPolicy);
 *
 * // Example of how this metadata drives API emission:
 * for (FieldModel f : fields) {
 *   String withName = "with" + capitalize(f.alias());
 *   // emit: public Builder withX(f.type() value) { ... }
 *   // if (f.isCollection()) also emit add/addAll/clear variants
 *   // if (f.asIdOnly() &amp;&amp; f.relationKind() == RelKind.TO_ONE) emit withXId(...)
 * }
 * </pre></blockquote>
 *
 * <h2>Semantics &amp; constraints</h2>
 * <ul>
 *   <li><strong>alias</strong> is the API-facing base name used for fluent methods (e.g. {@code withAlias(...)}).
 *       It may differ from {@code name} (e.g., boolean fields starting with {@code is...}).</li>
 *   <li><strong>setterName</strong> is the exact Java setter the generator attempts to call; if not
 *       available, field assignment may be performed via reflection.</li>
 *   <li><strong>isCollection / isList / isSet</strong> are mutually consistent flags describing the field shape.
 *       When {@code isCollection} is {@code true}, {@code elementType} holds the collection's element type
 *       (or {@code Object} if unknown/erased).</li>
 *   <li><strong>asIdOnly</strong> indicates that a relation should be exposed as an identifier in the
 *       builder API (e.g. {@code withEmployeeId(...)}) instead of an object reference.</li>
 *   <li><strong>relationKind</strong> classifies the field as {@code NONE}, {@code TO_ONE}, or {@code TO_MANY}
 *       for pre-persistence relation handling.</li>
 * </ul>
 *
 * <h2>Thread-safety</h2>
 * <p>This type is a Java {@code record} and therefore immutable and thread-safe by design.</p>
 *
 * @author Erik Pförtner
 * @since 1.0.0
 */
public record FieldModel(
        String name,
        TypeName type,
        String setterName,
        boolean primitive,
        boolean isBoolean,
        String alias,
        boolean asIdOnly,
        boolean isCollection,
        boolean isList,
        boolean isSet,
        TypeName elementType,
        RelKind relationKind
) {
}
