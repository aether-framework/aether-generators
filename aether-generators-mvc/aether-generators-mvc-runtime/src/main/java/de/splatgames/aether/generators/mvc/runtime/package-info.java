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
 * Runtime abstractions used by generated MVC test-data builders.
 *
 * <h2>Purpose</h2>
 * This module provides minimal, framework-agnostic contracts that allow generated builders to:
 * <ul>
 *   <li>persist object graphs when operating in a persistent mode, and</li>
 *   <li>optionally resolve supporting services at runtime.</li>
 * </ul>
 *
 * <h2>Key Types</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.generators.mvc.runtime.PersistAdapter} – a slim persistence facade
 *       that defines saving, optional lookup, and optional dependency resolution.</li>
 * </ul>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *   <li><strong>Minimal surface:</strong> the API is intentionally small to avoid coupling to any specific framework.</li>
 *   <li><strong>Transactions:</strong> transactional boundaries are the caller's responsibility.</li>
 *   <li><strong>Thread-safety:</strong> implementors should document their own thread-safety behavior.</li>
 *   <li><strong>Extensibility:</strong> adapters may expose repositories or helpers via {@code require(Class)}.</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <blockquote><pre>
 * // A simple adapter that saves entities using a custom repository
 * public final class SimpleAdapter implements PersistAdapter {
 *   private final Repo repo;
 *
 *   public SimpleAdapter(Repo repo) { this.repo = repo; }
 *
 *   &#64;Override public &lt;T&gt; @NotNull T save(@NotNull T entity) {
 *     return repo.save(entity); // return managed instance
 *   }
 *
 *   &#64;Override public &lt;T&gt; T findById(@NotNull Class&lt;T&gt; type, @NotNull Object id) {
 *     return repo.find(type, id); // or throw if unsupported
 *   }
 * }
 * </pre></blockquote>
 *
 * @author Erik Pförtner
 * @since 1.0.0
 */
package de.splatgames.aether.generators.mvc.runtime;
