/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.core.util.trie;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A Trie-based implementation for matching and storing paths.
 *
 * @param <T> The type of path objects stored in the Trie, which must extend the {@code Path} class.
 */
public class PathMatcherTrie<T extends Path> implements PathTrie<T> {

    private final Supplier<Character> delimiter;

    private final Supplier<List<T>> supplier;

    private volatile Map<String, T> cache;

    private volatile PathMatcher<T> matcher;

    /**
     * Constructs a {@code PathMatcherTrie} with the default delimiter.
     *
     * @param supplier A supplier providing a list of paths to be added to the Trie.
     */
    public PathMatcherTrie(Supplier<List<T>> supplier) {
        this(PathType.URL.getDelimiter(), supplier);
    }

    /**
     * Constructs a {@code PathMatcherTrie} with the specified delimiter.
     *
     * @param delimiter The delimiter used to separate path segments.
     * @param supplier  A supplier providing a list of paths to be added to the Trie.
     */
    public PathMatcherTrie(char delimiter, Supplier<List<T>> supplier) {
        this(() -> delimiter, supplier);
    }

    /**
     * Constructs a {@code PathMatcherTrie} with the specified delimiter supplier.
     *
     * @param delimiter A supplier providing the delimiter used to separate path segments.
     * @param supplier  A supplier providing a list of paths to be added to the Trie.
     */
    public PathMatcherTrie(Supplier<Character> delimiter, Supplier<List<T>> supplier) {
        this.delimiter = delimiter;
        this.supplier = supplier;
    }

    /**
     * Matches a given path against the Trie and returns the corresponding path object.
     *
     * @param path The path to be matched.
     * @param type The type of path matching to be performed.
     * @return The matched path object of type {@code T}, or {@code null} if no match is found.
     */
    @Override
    public T match(String path, PathMatchType type) {
        PathMatcher.MatchResult<T> result = getMatcher().match(path);
        if (result == null) {
            return null;
        } else if (type == null || type == PathMatchType.PREFIX) {
            return result.getValue();
        } else {
            return result.getType() == PathMatchType.EQUAL ? result.getValue() : null;
        }
    }

    /**
     * Retrieves the path object associated with the given path.
     *
     * @param path The path whose associated path object is to be returned.
     * @return The path object associated with the specified path, or {@code null} if no such path is found.
     */
    @Override
    public T get(String path) {
        return path == null ? null : getCache().get(path);
    }

    /**
     * Clears all the paths stored in the Trie.
     */
    @Override
    public void clear() {
        matcher = null;
    }

    /**
     * Retrieves the cache of paths, initializing it if necessary.
     *
     * @return The cache of paths.
     */
    private Map<String, T> getCache() {
        if (cache == null) {
            synchronized (this) {
                if (cache == null) {
                    Map<String, T> map = new HashMap<>();
                    if (supplier != null) {
                        List<T> paths = supplier.get();
                        if (paths != null) {
                            paths.forEach(path -> map.put(path.getPath(), path));
                        }
                    }
                    cache = map;
                }
            }
        }
        return cache;
    }

    /**
     * Lazily initializes and returns the underlying {@link PathMatcher}.
     *
     * @return The initialized {@link PathMatcher}.
     */
    private PathMatcher<T> getMatcher() {
        if (matcher == null) {
            synchronized (this) {
                if (matcher == null) {
                    PathMatcher<T> result = new PathMatcher<>(delimiter.get());
                    if (supplier != null) {
                        List<T> paths = supplier.get();
                        if (paths != null) {
                            paths.forEach(path -> result.addPath(path.getPath(), path));
                        }
                    }
                    matcher = result;
                }
            }
        }
        return matcher;
    }
}


