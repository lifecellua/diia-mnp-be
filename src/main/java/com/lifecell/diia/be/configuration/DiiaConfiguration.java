package com.lifecell.diia.be.configuration;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import lombok.NonNull;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;
import ua.gov.diia.documentsservice.DocumentsServiceGrpc;
import ua.gov.diia.grpc.healthcheck.GrpcHealthCheck;
import ua.gov.diia.grpc.interceptor.server.SessionServerInterceptor;
import ua.gov.diia.message.configuration.EnableDiiaBus;
import ua.gov.diia.metrics.config.EnableDiiaMetrics;
import ua.gov.diia.notification.NotificationServiceGrpc;
import ua.gov.diia.user.UserServiceGrpc;

import java.util.Base64;

import static com.google.common.base.Preconditions.checkNotNull;

@EnableDiiaMetrics
@EnableDiiaBus
@Configuration(proxyBeanMethods = false)
public class DiiaConfiguration {

    @Bean
    public DocumentsServiceGrpc.DocumentsServiceBlockingStub documentsServiceGrpc(GrpcChannelFactory channels) {
        return DocumentsServiceGrpc.newBlockingStub(channels.createChannel("diia-documents-service"))
                .withInterceptors(new HeaderAttachingClientInterceptor());
    }

    @Bean
    public UserServiceGrpc.UserServiceBlockingStub userServiceGrpc(GrpcChannelFactory channels) {
        return UserServiceGrpc.newBlockingStub(channels.createChannel("diia-user-service"))
                .withInterceptors(new HeaderAttachingClientInterceptor());
    }

    @Bean
    public NotificationServiceGrpc.NotificationServiceBlockingStub notificationServiceGrpc(GrpcChannelFactory channels) {
        return NotificationServiceGrpc.newBlockingStub(channels.createChannel("diia-notification-service"));
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.grpc.server.health", name = "enabled", havingValue = "false")
    public GrpcHealthCheck grpcHealthCheck(
            final @NonNull HealthEndpoint healthEndpoint,
            final @NonNull HealthContributorRegistry healthContributorRegistry
    ) {
        return new GrpcHealthCheck(healthEndpoint, healthContributorRegistry);
    }

    /*
    *  Look: MetadataUtils.newAttachHeadersInterceptor(metadata)
    */

    private static final class HeaderAttachingClientInterceptor implements ClientInterceptor {

        public HeaderAttachingClientInterceptor() {
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
            return new HeaderAttachingClientCall<>(next.newCall(method, callOptions));
        }

        private final class HeaderAttachingClientCall<ReqT, RespT> extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {

            public HeaderAttachingClientCall(ClientCall<ReqT, RespT> call) {
                super(call);
            }

            private Metadata reconstructMetadata() {
                var headers = new Metadata();
                {
                    var value = SessionServerInterceptor.SESSION_KEY.get();
                    if (value != null) {
                        try {
                            var json = JsonFormat.printer().print(value);
                            var encoded = Base64.getEncoder().encodeToString(json.getBytes());
                            headers.put(Metadata.Key.of("session", Metadata.ASCII_STRING_MARSHALLER), encoded);
                        } catch (InvalidProtocolBufferException e) {
                            //none
                        }
                    }
                }
                {
                    var value = SessionServerInterceptor.DEVICE_INFO_KEY.get();
                    if ((value != null) && (value.getPlatformType() != null)) {
                        var encoded = value.getPlatformType().name();
                        headers.put(Metadata.Key.of("platformtype", Metadata.ASCII_STRING_MARSHALLER), encoded);
                    }
                }
                {
                    var value = SessionServerInterceptor.DEVICE_INFO_KEY.get();
                    if ((value != null) && (value.getPlatformVersion() != null)) {
                        var encoded = value.getPlatformVersion();
                        headers.put(Metadata.Key.of("platformversion", Metadata.ASCII_STRING_MARSHALLER), encoded);
                    }
                }
                {
                    var value = SessionServerInterceptor.DEVICE_INFO_KEY.get();
                    if ((value != null) && (value.getAppVersion() != null)) {
                        var encoded = value.getAppVersion();
                        headers.put(Metadata.Key.of("appversion", Metadata.ASCII_STRING_MARSHALLER), encoded);
                    }
                }
                {
                    var value = SessionServerInterceptor.DEVICE_INFO_KEY.get();
                    if ((value != null) && (value.getMobileUid() != null)) {
                        var encoded = value.getMobileUid();
                        headers.put(Metadata.Key.of("mobileuid", Metadata.ASCII_STRING_MARSHALLER), encoded);
                    }
                }
//                {
//                    var value = SessionServerInterceptor.REQUEST_INFO_KEY.get();
//                    if ((value != null) && (value.getVersion() != null)) {
//                        var encoded = value.getVersion();
//                        headers.put(Metadata.Key.of("actionversion", Metadata.ASCII_STRING_MARSHALLER), encoded);
//                    }
//                }
                {
                    var value = SessionServerInterceptor.REQUEST_INFO_KEY.get();
                    if ((value != null) && (value.getTraceId() != null) && (!value.getTraceId().isBlank())) {
                        var encoded = value.getTraceId();
                        headers.put(Metadata.Key.of("traceid", Metadata.ASCII_STRING_MARSHALLER), encoded);
                    }
                }
                return headers;
            }

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.merge(reconstructMetadata());
                super.start(responseListener, headers);
            }
        }
    }
}
