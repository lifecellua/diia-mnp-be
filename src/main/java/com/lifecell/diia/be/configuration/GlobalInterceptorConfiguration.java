package com.lifecell.diia.be.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GlobalClientInterceptor;
import org.springframework.grpc.server.GlobalServerInterceptor;
import ua.gov.diia.grpc.interceptor.client.GrpcClientMetricInterceptor;
import ua.gov.diia.grpc.interceptor.server.ExceptionHandler;
import ua.gov.diia.grpc.interceptor.server.GrpcServerMetricAndLogInterceptor;
import ua.gov.diia.grpc.interceptor.server.SessionServerInterceptor;
import ua.gov.diia.metrics.config.DiiaMetricsProperties;

@Configuration(proxyBeanMethods = false)
public class GlobalInterceptorConfiguration {

    @Bean
    @GlobalServerInterceptor
    GrpcServerMetricAndLogInterceptor grpcMetricInterceptor(final @NonNull MeterRegistry meterRegistry, final @NonNull DiiaMetricsProperties diiaMetricsProperties) {
        return new GrpcServerMetricAndLogInterceptor(meterRegistry, diiaMetricsProperties);
    }

    @Bean
    @GlobalServerInterceptor
    SessionServerInterceptor sessionServerInterceptor() {
        return new SessionServerInterceptor();
    }

    @Bean
    @GlobalServerInterceptor
    ExceptionHandler exceptionHandler() {
        return new ExceptionHandler();
    }

    @Bean
    @GlobalClientInterceptor
    GrpcClientMetricInterceptor grpcClientMetricInterceptor(final @NonNull MeterRegistry meterRegistry,
                                                            final @NonNull DiiaMetricsProperties diiaMetricsProperties) {
        return new GrpcClientMetricInterceptor(meterRegistry, diiaMetricsProperties);
    }
}
