package ir.tax.trustedtaxpayers.application.config;

import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import net.devh.boot.grpc.server.interceptor.GlobalServerInterceptorConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class GrpcServerConfig {

    private final MeterRegistry meterRegistry;

    public GrpcServerConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Bean
    public GlobalServerInterceptorConfigurer globalInterceptorConfigurerAdapter() {
        return registry -> {
            registry.add(new IdempotencyInterceptor());
            registry.add(new ExceptionMappingInterceptor());
            registry.add(new DurationMeasureInterceptor(meterRegistry));
        };
    }

    static class IdempotencyInterceptor implements ServerInterceptor {
        private static final Metadata.Key<String> IDEMPOTENCY_KEY = Metadata.Key.of("idempotency-key", Metadata.ASCII_STRING_MARSHALLER);
        private static final ConcurrentHashMap<String, String> processedKeys = new ConcurrentHashMap<>(1000);

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
            String idempotencyKey = headers.get(IDEMPOTENCY_KEY);
            if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                String existing = processedKeys.putIfAbsent(idempotencyKey, call.getMethodDescriptor().getFullMethodName());
                if (existing != null) {
                    call.close(Status.ALREADY_EXISTS.withDescription("Duplicate request with idempotency key: " + idempotencyKey), new Metadata());
                    return new ServerCall.Listener<>() {};
                }
            }
            ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);
            return new SimpleForwardingServerCallListener<>(delegate) {
                @Override
                public void onComplete() {
                    if (idempotencyKey != null) {
                        processedKeys.remove(idempotencyKey);
                    }
                    super.onComplete();
                }
            };
        }
    }

    static class ExceptionMappingInterceptor implements ServerInterceptor {
        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
            ServerCall<ReqT, RespT> safeCall = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {};
            ServerCall.Listener<ReqT> delegate = next.startCall(safeCall, headers);
            return new SimpleForwardingServerCallListener<>(delegate) {
                @Override
                public void onHalfClose() {
                    try {
                        super.onHalfClose();
                    } catch (IllegalArgumentException e) {
                        call.close(Status.INVALID_ARGUMENT.withDescription(e.getMessage()), new Metadata());
                    } catch (SecurityException e) {
                        call.close(Status.PERMISSION_DENIED.withDescription(e.getMessage()), new Metadata());
                    } catch (Throwable t) {
                        call.close(Status.INTERNAL.withDescription(t.getMessage()), new Metadata());
                    }
                }
            };
        }
    }

    static class DurationMeasureInterceptor implements ServerInterceptor {
        private final MeterRegistry meterRegistry;

        DurationMeasureInterceptor(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
            String methodName = call.getMethodDescriptor().getFullMethodName();
            Timer.Sample sample = Timer.start(meterRegistry);
            ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);
            return new SimpleForwardingServerCallListener<>(delegate) {
                @Override
                public void onComplete() {
                    sample.stop(Timer.builder("grpc.server.duration")
                            .tag("method", methodName)
                            .tag("status", "success")
                            .register(meterRegistry));
                    super.onComplete();
                }

                @Override
                public void onCancel() {
                    sample.stop(Timer.builder("grpc.server.duration")
                            .tag("method", methodName)
                            .tag("status", "cancelled")
                            .register(meterRegistry));
                    super.onCancel();
                }
            };
        }
    }
}
