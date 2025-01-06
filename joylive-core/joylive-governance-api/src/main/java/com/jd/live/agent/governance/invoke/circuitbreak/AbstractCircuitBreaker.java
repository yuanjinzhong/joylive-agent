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
package com.jd.live.agent.governance.invoke.circuitbreak;

import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakPolicy;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AbstractCircuitBreaker
 *
 * @since 1.1.0
 */
public abstract class AbstractCircuitBreaker implements CircuitBreaker {

    @Getter
    protected final CircuitBreakPolicy policy;

    @Getter
    protected final URI uri;

    @Getter
    protected long lastAccessTime;

    protected final AtomicBoolean started = new AtomicBoolean(true);

    public AbstractCircuitBreaker(CircuitBreakPolicy policy, URI uri) {
        this.policy = policy;
        this.uri = uri;
    }

    @Override
    public boolean acquire() {
        if (!started.get()) {
            return true;
        }
        lastAccessTime = System.currentTimeMillis();
        return doAcquire();
    }

    @Override
    public void release() {
        if (started.get()) {
            doRelease();
        }
    }

    @Override
    public void onError(long durationInMs, Throwable throwable) {
        if (started.get()) {
            doOnError(durationInMs, throwable);
        }
    }

    @Override
    public void onSuccess(long durationInMs) {
        if (started.get()) {
            doOnSuccess(durationInMs);
        }
    }

    @Override
    public void close() {
        // When the circuit breaker is not accessed for a long time, it will be automatically garbage collected.
        if (started.compareAndSet(true, false)) {
            doClose();
        }
    }

    /**
     * Closes the circuit breaker.
     */
    protected void doClose() {

    }

    /**
     * Performs the actual acquisition logic.
     * Subclasses must implement this method to define the specific acquisition behavior.
     *
     * @return true if the acquisition is successful, false otherwise.
     */
    protected abstract boolean doAcquire();

    /**
     * Releases the acquired permit.
     */
    protected abstract void doRelease();

    /**
     * Records a failed call. This method should be invoked when a call fails.
     *
     * @param durationInMs The elapsed time duration of the call in milliseconds.
     * @param throwable    The throwable that represents the failure.
     */
    protected abstract void doOnError(long durationInMs, Throwable throwable);

    /**
     * Records a successful call. This method should be invoked when a call is successful.
     *
     * @param durationInMs The elapsed time duration of the call in milliseconds.
     */
    protected abstract void doOnSuccess(long durationInMs);

}
