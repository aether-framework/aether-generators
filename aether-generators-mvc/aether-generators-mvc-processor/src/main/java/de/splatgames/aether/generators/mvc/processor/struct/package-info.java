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

/**
 * Lightweight model structures used by the MVC builder processor.
 *
 * <p>This package provides internal value objects that represent
 * metadata extracted from entity classes during annotation processing.</p>
 *
 * <ul>
 *   <li>{@link de.splatgames.aether.generators.mvc.processor.struct.FieldModel} –
 *       description of a single field, its type, alias, relation, and flags</li>
 *   <li>{@link de.splatgames.aether.generators.mvc.processor.struct.RelKind} –
 *       enumeration of supported relation kinds (none / to-one / to-many)</li>
 * </ul>
 *
 * <p>These types are not user-facing but support the code generation logic.</p>
 *
 * @since 1.0.0
 */
package de.splatgames.aether.generators.mvc.processor.struct;