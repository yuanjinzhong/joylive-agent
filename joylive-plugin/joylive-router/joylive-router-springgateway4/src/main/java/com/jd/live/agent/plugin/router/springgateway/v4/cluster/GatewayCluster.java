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
package com.jd.live.agent.plugin.router.springgateway.v4.cluster;

import com.jd.live.agent.bootstrap.exception.RejectException.RejectCircuitBreakException;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.core.util.type.ClassDesc;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.core.util.type.FieldDesc;
import com.jd.live.agent.core.util.type.FieldList;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.plugin.router.springcloud.v3.cluster.AbstractClientCluster;
import com.jd.live.agent.plugin.router.springcloud.v3.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springgateway.v4.request.GatewayClusterRequest;
import com.jd.live.agent.plugin.router.springgateway.v4.response.GatewayClusterResponse;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory.RetryConfig;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.jd.live.agent.bootstrap.exception.RejectException.RejectCircuitBreakException.getCircuitBreakException;
import static org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools.reconstructURI;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

@Getter
public class GatewayCluster extends AbstractClientCluster<GatewayClusterRequest, GatewayClusterResponse> {

    private static final Logger logger = LoggerFactory.getLogger(GatewayCluster.class);

    private static final String FIELD_CLIENT_FACTORY = "clientFactory";

    private final LoadBalancerClientFactory clientFactory;

    public GatewayCluster(GlobalFilter filter) {
        ClassDesc describe = ClassUtils.describe(filter.getClass());
        FieldList fieldList = describe.getFieldList();
        FieldDesc field = fieldList.getField(FIELD_CLIENT_FACTORY);
        this.clientFactory = (LoadBalancerClientFactory) (field == null ? null : field.get(filter));
    }

    @Override
    public ClusterPolicy getDefaultPolicy(GatewayClusterRequest request) {
        RetryConfig retryConfig = request.getRetryConfig();
        if (retryConfig != null && retryConfig.getRetries() > 0) {
            RetryGatewayFilterFactory.BackoffConfig backoff = retryConfig.getBackoff();
            RetryPolicy retryPolicy = new RetryPolicy();
            retryPolicy.setRetry(retryConfig.getRetries());
            retryPolicy.setRetryInterval(backoff != null ? backoff.getFirstBackoff().toMillis() : null);
            retryPolicy.setRetryStatuses(retryConfig.getStatuses().stream().map(v -> String.valueOf(v.value())).collect(Collectors.toSet()));
            retryPolicy.setRetryExceptions(retryConfig.getExceptions().stream().map(v -> v.getClass().getName()).collect(Collectors.toSet()));
            return new ClusterPolicy(ClusterInvoker.TYPE_FAILOVER, retryPolicy);
        }
        return new ClusterPolicy(ClusterInvoker.TYPE_FAILFAST);
    }

    @Override
    protected boolean isRetryable() {
        return true;
    }

    @Override
    public CompletionStage<GatewayClusterResponse> invoke(GatewayClusterRequest request, SpringEndpoint endpoint) {
        try {
            Mono<Void> mono = request.getChain().filter(request.getExchange());
            return mono.toFuture().thenApply(v -> new GatewayClusterResponse(request.getExchange().getResponse()));
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    @Override
    public GatewayClusterResponse createResponse(Throwable throwable, GatewayClusterRequest request, SpringEndpoint endpoint) {
        RejectCircuitBreakException circuitBreakException = getCircuitBreakException(throwable);
        if (circuitBreakException != null) {
            DegradeConfig config = circuitBreakException.getConfig();
            if (config != null) {
                try {
                    return new GatewayClusterResponse(createResponse(request, config));
                } catch (Throwable e) {
                    logger.warn("Exception occurred when create degrade response from circuit break. caused by " + e.getMessage(), e);
                    return new GatewayClusterResponse(createException(throwable, request, endpoint));
                }
            }
        }
        return new GatewayClusterResponse(createException(throwable, request, endpoint));
    }

    @Override
    public void onStartRequest(GatewayClusterRequest request, SpringEndpoint endpoint) {
        if (endpoint != null) {
            ServiceInstance instance = endpoint.getInstance();
            URI uri = request.getRequest().getURI();
            // if the `lb:<scheme>` mechanism was used, use `<scheme>` as the default,
            // if the loadbalancer doesn't provide one.
            String overrideScheme = instance.isSecure() ? "https" : "http";
            Map<String, Object> attributes = request.getExchange().getAttributes();
            String schemePrefix = (String) attributes.get(GATEWAY_SCHEME_PREFIX_ATTR);
            if (schemePrefix != null) {
                overrideScheme = request.getURI().getScheme();
            }
            URI requestUrl = reconstructURI(new DelegatingServiceInstance(instance, overrideScheme), uri);

            attributes.put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
            attributes.put(GATEWAY_LOADBALANCER_RESPONSE_ATTR, endpoint.getResponse());
        }
        super.onStartRequest(request, endpoint);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(GatewayClusterResponse response, GatewayClusterRequest request, SpringEndpoint endpoint) {
        ServerWebExchange exchange = request.getExchange();
        request.lifecycles(l -> l.onComplete(new CompletionContext<>(
                CompletionContext.Status.SUCCESS,
                request.getLbRequest(),
                endpoint.getResponse(),
                new ResponseData(exchange.getResponse(),
                        new RequestData(exchange.getRequest(), exchange.getAttributes())))));
    }

    /**
     * Creates a {@link ServerHttpResponse} based on the provided {@link GatewayClusterRequest} and {@link DegradeConfig}.
     * The response is configured with the status code, headers, and body specified in the degrade configuration.
     *
     * @param httpRequest   the original HTTP request containing headers.
     * @param degradeConfig the degrade configuration specifying the response details such as status code, headers, and body.
     * @return a {@link ServerHttpResponse} configured according to the degrade configuration.
     */
    private ServerHttpResponse createResponse(GatewayClusterRequest httpRequest, DegradeConfig degradeConfig) {
        ServerHttpResponse response = httpRequest.getExchange().getResponse();
        ServerHttpRequest request = httpRequest.getExchange().getRequest();

        String body = degradeConfig.getResponseBody();
        int length = body == null ? 0 : body.length();
        byte[] bytes = length == 0 ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        HttpHeaders headers = response.getHeaders();
        headers.putAll(request.getHeaders());
        Map<String, String> attributes = degradeConfig.getAttributes();
        if (attributes != null) {
            attributes.forEach(headers::add);
        }
        response.setRawStatusCode(degradeConfig.getResponseCode());
        response.setStatusCode(HttpStatus.valueOf(degradeConfig.getResponseCode()));
        headers.set(HttpHeaders.CONTENT_TYPE, degradeConfig.getContentType());

        response.writeWith(Flux.just(buffer)).block();
        return response;
    }

}
