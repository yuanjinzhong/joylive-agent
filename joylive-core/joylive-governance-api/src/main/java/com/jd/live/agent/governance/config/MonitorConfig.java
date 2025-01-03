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
package com.jd.live.agent.governance.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * monitor configuration.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonitorConfig {

    private Boolean forwardEnabled;

    private Boolean rejectEnabled = true;

    public boolean isForward() {
        return forwardEnabled != null && forwardEnabled;
    }

    public boolean isReject() {
        return rejectEnabled == null || rejectEnabled;
    }
}

