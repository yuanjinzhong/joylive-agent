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
package com.jd.live.agent.core.util.template;

import java.util.Map;

/**
 * An interface defining the contract for evaluating expressions or scripts in a given context.
 */
public interface Evaluator {

    /**
     * Evaluates an expression or script based on the provided context.
     *
     * @param context A map containing context variables that may be used during the evaluation.
     * @return The result of the evaluation as a String.
     */
    String evaluate(Map<String, Object> context);
}
