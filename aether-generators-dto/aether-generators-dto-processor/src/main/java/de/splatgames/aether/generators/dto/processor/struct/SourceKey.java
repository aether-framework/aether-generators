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

package de.splatgames.aether.generators.dto.processor.struct;

import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.TypeElement;
import java.util.Objects;

/**
 * Stable map/set key for grouping annotated members by their declaring source type.
 * <p>
 * A {@code SourceKey} encapsulates a {@link TypeElement} and its fully qualified name (FQN).
 * Equality and hash code are deliberately based <strong>only</strong> on the FQN, making the key
 * robust across annotation processing rounds and independent of {@code TypeElement} identity
 * semantics. The original {@code TypeElement} is retained for diagnostics and as an originating
 * element when emitting generated sources.
 * </p>
 *
 * <h2>Intended usage</h2>
 * <p>
 * The DTO processor uses {@code SourceKey} as the key in a map from source types to the lists
 * of annotated fields. This ensures deterministic grouping and avoids subtle issues caused by
 * comparing {@code TypeElement} instances from different rounds or utilities.
 * </p>
 *
 * @implNote
 * This key treats nested classes by their canonical FQN as returned by
 * {@link TypeElement#getQualifiedName()}. Two keys are considered equal if and only if their FQNs
 * are equal as strings.
 *
 * @see javax.lang.model.element.TypeElement
 * @see de.splatgames.aether.generators.dto.processor.DtoProcessor
 * @see de.splatgames.aether.generators.dto.processor.struct.DtoGroup
 *
 * @since 1.1.0
 * @author Erik Pförtner
 */
public final class SourceKey {
    private final TypeElement source;
    private final String fqn;

    /**
     * Creates a new source key for the given {@link TypeElement}.
     *
     * @param source the declaring type element (never {@code null})
     */
    public SourceKey(@NotNull final TypeElement source) {
        this.source = source;
        this.fqn = source.getQualifiedName().toString();
    }

    /**
     * Returns the underlying {@link TypeElement} for diagnostics and as an originating element.
     *
     * @return the type element (never {@code null})
     */
    @NotNull
    public TypeElement getSource() {
        return this.source;
    }

    /**
     * Returns the fully qualified name of the declaring type.
     *
     * @return the FQN string (never {@code null})
     */
    @NotNull
    public String getFqn() {
        return this.fqn;
    }

    /**
     * Compares keys by fully qualified name.
     *
     * @param o another object
     * @return {@code true} iff {@code o} is a {@code SourceKey} with the same FQN
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof SourceKey sk && sk.fqn.equals(this.fqn);
    }

    /**
     * Computes a hash code from the fully qualified name.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.fqn);
    }
}
