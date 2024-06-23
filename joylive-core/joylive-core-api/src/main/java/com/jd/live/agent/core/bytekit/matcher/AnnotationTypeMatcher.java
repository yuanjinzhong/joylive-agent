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
package com.jd.live.agent.core.bytekit.matcher;

import com.jd.live.agent.core.bytekit.type.AnnotationDesc;
import com.jd.live.agent.core.bytekit.type.TypeDesc;


/**
 * AnnotationTypeMatcher
 *
 * @param <T> Match target type
 * @since 1.0.0
 */
public class AnnotationTypeMatcher<T extends AnnotationDesc> extends AbstractJunction<T> {

    private final ElementMatcher<? super TypeDesc> matcher;

    public AnnotationTypeMatcher(ElementMatcher<? super TypeDesc> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean match(T target) {
        return matcher.match(target.getAnnotationType());
    }
}
