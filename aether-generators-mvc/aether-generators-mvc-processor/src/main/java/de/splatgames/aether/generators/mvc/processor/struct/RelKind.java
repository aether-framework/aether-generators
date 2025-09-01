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

/**
 * Relation classification used by the code generator.
 *
 * <p>This enum describes whether a field participates in an object relationship and,
 * if so, whether the relation is single-valued or multi-valued. The classification
 * is derived from annotations encountered during processing (e.g., strings like
 * {@code "jakarta.persistence.OneToOne"}, {@code "jakarta.persistence.ManyToOne"},
 * {@code "jakarta.persistence.OneToMany"}, {@code "jakarta.persistence.ManyToMany"}),
 * or defaults to {@link #NONE} when no relation is detected.</p>
 *
 * <h2>Usage</h2>
 * <blockquote><pre>
 * // Emission of builder API:
 * switch (field.relationKind()) {
 *   case NONE:
 *     // emit withX(...)
 *     break;
 *   case TO_ONE:
 *     // emit withX(...) or withXId(...) when asIdOnly=true
 *     break;
 *   case TO_MANY:
 *     // emit withX(...), addX(E), addAllX(Collection&lt;? extends E&gt;), clearX()
 *     break;
 * }
 *
 * // Pre-persistence handling (pseudo):
 * if (field.relationKind() == RelKind.TO_ONE) {
 *   ensurePersistent(getField(obj, field.name()), depth - 1, visited);
 * } else if (field.relationKind() == RelKind.TO_MANY) {
 *   for (Object e : getCollection(obj, field.name())) ensurePersistent(e, depth - 1, visited);
 * }
 * </pre></blockquote>
 *
 * <h2>Notes</h2>
 * <ul>
 *   <li>{@link #NONE} means the field is treated like a scalar for code generation.</li>
 *   <li>{@link #TO_ONE} covers single-reference relationships (e.g., OneToOne / ManyToOne).</li>
 *   <li>{@link #TO_MANY} covers collection relationships (e.g., OneToMany / ManyToMany).</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @since 1.0.0
 */
public enum RelKind {

    /**
     * No relationship: the field is treated as a scalar value during generation
     * and does not trigger relation-specific pre-persistence logic.
     */
    NONE,

    /**
     * Single-valued relationship (e.g., OneToOne or ManyToOne).
     * May cause the generator to ensure the referenced object is persisted
     * prior to saving the root entity.
     */
    TO_ONE,

    /**
     * Multi-valued relationship (e.g., OneToMany or ManyToMany).
     * May cause the generator to iterate elements and ensure each is persisted
     * prior to saving the root entity.
     */
    TO_MANY
}
