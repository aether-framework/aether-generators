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
 * Annotation processor for {@code @MvcBuilder}.
 *
 * <p>This package contains the main {@link javax.annotation.processing.Processor}
 * implementation that inspects entity classes annotated with
 * {@link de.splatgames.aether.generators.mvc.annotations.MvcBuilder} and generates
 * strongly-typed builder classes using {@link com.squareup.javapoet.JavaFile}.
 *
 * <ul>
 *   <li>Scans entity fields and detects relations</li>
 *   <li>Applies {@link de.splatgames.aether.generators.mvc.annotations.MvcField} /
 *       {@link de.splatgames.aether.generators.mvc.annotations.MvcIgnore}</li>
 *   <li>Generates fluent API methods (with/add/clear)</li>
 *   <li>Supports defaults providers and persistence integration</li>
 * </ul>
 *
 * @since 1.0.0
 */
package de.splatgames.aether.generators.mvc.processor;
