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

import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Deterministic generator for {@code serialVersionUID} values of emitted DTO classes.
 *
 * <p>Unlike the default JVM toolchain algorithm for computing a {@code serialVersionUID},
 * this utility derives a stable 64-bit value from a compact, well-defined fingerprint of
 * the generated DTO's <em>public identity</em>:
 * <ul>
 *   <li>the target package name,</li>
 *   <li>the DTO simple type name,</li>
 *   <li>for each field (in generation order): the field name and its <em>erased</em> type name.</li>
 * </ul>
 * The fingerprint is hashed using {@code SHA-256}; the first 8 bytes of the digest are then
 * interpreted as a signed {@code long} (big-endian) to produce the {@code serialVersionUID}.</p>
 *
 * <h2>Stability &amp; compatibility</h2>
 * <p>
 * The result is deterministic across builds as long as the inputs remain unchanged. Any change to:
 * package, DTO name, field set, field names, or erased types will yield a different value.
 * This is intentional: structural changes that affect serialization shape will invalidate the
 * previous stream format to avoid accidental deserialization with incompatible layouts.
 * </p>
 *
 * @implNote This algorithm is a <em>custom scheme</em> designed to be reproducible and simple.
 * It does <strong>not</strong> replicate the JDK's reflective serialVersionUID computation.
 * The byte order used by {@link ByteBuffer} is the default big-endian.
 * @see java.io.Serializable
 * @see DtoGroup
 * @see DtoField
 * @since 1.1.0
 * author Erik Pförtner
 */
public final class SerialUid {
    private SerialUid() {
    }

    /**
     * Computes a deterministic {@code serialVersionUID} for the given DTO group.
     *
     * <p>The hash input concatenates:
     * <ol>
     *   <li>{@code targetPackage}</li>
     *   <li>{@code dtoSimpleName}</li>
     *   <li>For each field (in the group's current order): {@code fieldName + ':' + erasedType}</li>
     * </ol>
     * Each component is separated by a single {@code '|'} byte to reduce ambiguity.</p>
     *
     * @param types the {@link Types} utility used to obtain erased type names (never {@code null})
     * @param g     the DTO group describing the target type and its fields (never {@code null})
     * @return the computed {@code serialVersionUID} as {@code long}
     * @throws IllegalStateException if the {@code SHA-256} algorithm is unavailable
     */
    public static long compute(@NotNull final Types types, @NotNull final DtoGroup g) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(g.getTargetPackage().getBytes());
            md.update((byte) '|');
            md.update(g.getDtoSimpleName().getBytes());
            for (DtoField f : g.fields) {
                md.update((byte) '|');
                md.update(f.getName().getBytes());
                md.update((byte) ':');
                md.update(erasedName(types, f.getType()).getBytes());
            }
            byte[] h = md.digest();
            ByteBuffer bb = ByteBuffer.wrap(Arrays.copyOf(h, 8));
            return bb.getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the erased type name for the given {@link TypeMirror}.
     *
     * @param types the {@link Types} utility (never {@code null})
     * @param t     the type mirror (never {@code null})
     * @return the erased type's {@code toString()} representation
     */
    @NotNull
    private static String erasedName(@NotNull final Types types, @NotNull final TypeMirror t) {
        return types.erasure(t).toString();
    }
}
