package com.mdb.user_data_gateway_service.grpc;

import com.mdb.user_data_gateway_service.producer.UserAccountCreatedEvent;
import com.mdb.user_data_gateway_service.producer.UserAccountCreatedProducer;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import java.time.Instant;
import java.util.UUID;

@GrpcService
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private final UserAccountCreatedProducer producer;

    public UserServiceImpl(UserAccountCreatedProducer producer) {
        this.producer = producer;
    }

    @Override
    public void triggerUserAccountCreated(UserAccountCreatedRequest request, StreamObserver<UserAccountCreatedResponse> responseObserver) {
        try {
            UUID userId = UUID.fromString(request.getUserId());
            UUID accountId = UUID.fromString(request.getAccountId());
            
            UserAccountCreatedEvent event = new UserAccountCreatedEvent(
                userId,
                accountId,
                request.getUsername(),
                request.getEmail(),
                Instant.now()
            );
            
            producer.send(event);
            
            UserAccountCreatedResponse response = UserAccountCreatedResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Event produced successfully")
                .build();
                
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            UserAccountCreatedResponse response = UserAccountCreatedResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Invalid UUID format: " + e.getMessage())
                .build();
                
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
