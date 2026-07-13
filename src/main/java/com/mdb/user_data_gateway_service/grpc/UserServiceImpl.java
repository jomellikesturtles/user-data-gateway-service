package com.mdb.user_data_gateway_service.grpc;

import com.mdb.user_data_gateway_service.entity.identity.Account;
import com.mdb.user_data_gateway_service.entity.identity.User;
import com.mdb.user_data_gateway_service.repository.identity.AccountRepository;
import com.mdb.user_data_gateway_service.repository.identity.UserRepository;
import com.mdb.user_data_gateway_service.producer.UserAccountCreatedEvent;
import com.mdb.user_data_gateway_service.producer.UserAccountCreatedProducer;
import com.mdb.user_data_gateway_service.utils.PasswordUtils;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

@GrpcService
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final UserAccountCreatedProducer kafkaProducer;
    private final TransactionTemplate transactionTemplate;
    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    public UserServiceImpl(UserRepository userRepository, 
                           AccountRepository accountRepository, 
                           UserAccountCreatedProducer kafkaProducer,
                           @Qualifier("identityTransactionTemplate") TransactionTemplate transactionTemplate) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.kafkaProducer = kafkaProducer;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void triggerUserAccountCreated(UserAccountCreatedRequest request, StreamObserver<UserAccountCreatedResponse> responseObserver) {
        LOGGER.info("Start UserServiceImpl triggerUserAccountCreated {}", request.getUserId());
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
            
            kafkaProducer.send(event);
            
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

    @Override
    public void getUserById(UserRequest request, StreamObserver<UserResponse> responseObserver) {
        LOGGER.info("getUserById request received with ID: '{}'", request.getId());
        try {
            if (request.getId() == null || request.getId().isEmpty()) {
                LOGGER.warn("Received empty user ID in getUserById request");
                responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                        .withDescription("User ID cannot be empty")
                        .asRuntimeException());
                return;
            }
            UUID userId = UUID.fromString(request.getId());
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                LOGGER.warn("User not found with ID: {}", request.getId());
                responseObserver.onError(io.grpc.Status.NOT_FOUND
                        .withDescription("User not found with ID: " + request.getId())
                        .asRuntimeException());
                return;
            }
            LOGGER.info("User found: {} (ID: {})", user.getUserName(), request.getId());
            responseObserver.onNext(mapToProto(user));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid UUID format for ID: '{}'", request.getId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID format: " + e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            LOGGER.error("Error in getUserById for ID: '{}'", request.getId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getUserByUsername(UserRequest request, StreamObserver<UserResponse> responseObserver) {
        LOGGER.info("getUserByUsername request received for username: '{}'", request.getUsername());
        try {
            if (request.getUsername() == null || request.getUsername().isEmpty()) {
                LOGGER.warn("Received empty username in getUserByUsername request");
                responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                        .withDescription("Username cannot be empty")
                        .asRuntimeException());
                return;
            }
            User user = userRepository.findByUserName(request.getUsername());
            if (user == null) {
                LOGGER.warn("User not found with username: {}", request.getUsername());
                responseObserver.onError(io.grpc.Status.NOT_FOUND
                        .withDescription("User not found with username: " + request.getUsername())
                        .asRuntimeException());
                return;
            }
            LOGGER.info("User found by username: {} (ID: {})", user.getUserName(), user.getUid());
            responseObserver.onNext(mapToProto(user));
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in getUserByUsername for username: '{}'", request.getUsername(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getUserByEmailOrUsername(UserRequest request, StreamObserver<UserResponse> responseObserver) {
        LOGGER.info("getUserByEmailOrUsername request received for email: '{}', username: '{}'", request.getEmail(), request.getUsername());
        try {
            User user = userRepository.findByEmailOrUserName(request.getEmail(), request.getUsername());
            if (user == null) {
                LOGGER.warn("User not found with email: '{}' or username: '{}'", request.getEmail(), request.getUsername());
                responseObserver.onError(io.grpc.Status.NOT_FOUND
                        .withDescription("User not found with email or username")
                        .asRuntimeException());
                return;
            }
            LOGGER.info("User found by email/username: {} (ID: {})", user.getUserName(), user.getUid());
            responseObserver.onNext(mapToProto(user));
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in getUserByEmailOrUsername for email: '{}', username: '{}'", request.getEmail(), request.getUsername(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void authenticate(AuthenticateRequest request, StreamObserver<UserResponse> responseObserver) {
        LOGGER.info("Authenticate request received for identifier: '{}'", request.getEmailOrUsername());
        try {
            User user = userRepository.findByEmailOrUserName(request.getEmailOrUsername(), request.getEmailOrUsername());
            if (user == null || !PasswordUtils.check(request.getPassword(), user.getPassword())) {
                LOGGER.warn("Authentication failed for identifier: '{}' (user not found or invalid password)", request.getEmailOrUsername());
                responseObserver.onError(io.grpc.Status.UNAUTHENTICATED.withDescription("Invalid credentials").asRuntimeException());
                return;
            }
            LOGGER.info("Authentication successful for user: {} (ID: {})", user.getUserName(), user.getUid());
            responseObserver.onNext(mapToProto(user));
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error during authentication for identifier: '{}'", request.getEmailOrUsername(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void registerUser(RegisterRequest request, StreamObserver<ActionResponse> responseObserver) {
        LOGGER.info("RegisterUser request received for username: '{}', email: '{}'", request.getUsername(), request.getEmail());
        try {
            // Check if user already exists
            if (userRepository.findByEmailOrUserName(request.getEmail(), request.getUsername()) != null) {
                LOGGER.warn("Registration failed: Username '{}' or Email '{}' already exists", request.getUsername(), request.getEmail());
                responseObserver.onNext(ActionResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Username or Email already exists")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            UserAccountCreatedEvent event = transactionTemplate.execute(status -> {
                try {
                    // Create account if not present or resolve UUID
                    UUID accountId = request.getAccountId().isEmpty() 
                            ? UUID.randomUUID() 
                            : UUID.fromString(request.getAccountId());
                    
                    if (request.getAccountId().isEmpty() || !accountRepository.existsById(accountId)) {
                        LOGGER.info("Creating new account with ID: {} for email: {}", accountId, request.getEmail());
                        Account account = Account.builder()
                                .id(accountId)
                                .email(request.getEmail())
                                .status("ACTIVE")
                                .build();
                        accountRepository.saveAndFlush(account);
                    }

                    // Save user
                    String hashedPassword = PasswordUtils.getSaltedHash(request.getPassword());
                    User user = User.builder()
                            .userName(request.getUsername())
                            .email(request.getEmail())
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .password(hashedPassword)
                            .isActive(true)
                            .accountId(accountId)
                            .roles(Collections.singletonList("ROLE_USER"))
                            .build();
                    userRepository.saveAndFlush(user);
                    LOGGER.info("Saved user '{}' successfully with ID: {}", user.getUserName(), user.getUid());

                    return new UserAccountCreatedEvent(
                            user.getUid(),
                            accountId,
                            user.getUserName(),
                            user.getEmail(),
                            Instant.now()
                    );
                } catch (Exception e) {
                    LOGGER.error("Failed to execute registration transaction for user: '{}'", request.getUsername(), e);
                    throw new RuntimeException(e);
                }
            });

            // Publish Event to Kafka only after transaction commits successfully
            if (event != null) {
                LOGGER.info("Publishing UserAccountCreatedEvent for user: {}", event.username());
                kafkaProducer.send(event);
            }

            responseObserver.onNext(ActionResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("User registered and account created successfully")
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in registerUser for user: '{}'", request.getUsername(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void updateUser(UserResponse request, StreamObserver<UserResponse> responseObserver) {
        LOGGER.info("UpdateUser request received for ID: {}", request.getId());
        try {
            User updatedUser = transactionTemplate.execute(status -> {
                try {
                    UUID userId = UUID.fromString(request.getId());
                    User user = userRepository.findById(userId).orElse(null);
                    if (user == null) {
                        LOGGER.warn("Update failed: user not found with ID: {}", request.getId());
                        throw new io.grpc.StatusRuntimeException(io.grpc.Status.NOT_FOUND);
                    }

                    // Update allowed fields
                    if (!request.getUsername().isEmpty()) user.setUserName(request.getUsername());
                    if (!request.getEmail().isEmpty()) user.setEmail(request.getEmail());
                    if (!request.getFirstName().isEmpty()) user.setFirstName(request.getFirstName());
                    if (!request.getLastName().isEmpty()) user.setLastName(request.getLastName());
                    if (!request.getAccountId().isEmpty()) user.setAccountId(UUID.fromString(request.getAccountId()));
                    user.setIsActive(request.getIsActive());
                    if (request.getRolesCount() > 0) {
                        if (user.getRoles() == null) {
                            user.setRoles(new java.util.ArrayList<>(request.getRolesList()));
                        } else {
                            user.getRoles().clear();
                            user.getRoles().addAll(request.getRolesList());
                        }
                    }

                    userRepository.saveAndFlush(user);
                    LOGGER.info("Successfully updated user with ID: {}", request.getId());
                    return user;
                } catch (io.grpc.StatusRuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    LOGGER.error("Failed to execute update transaction for user ID: {}", request.getId(), e);
                    throw new RuntimeException(e);
                }
            });

            if (updatedUser == null) {
                LOGGER.error("Update failed: transactional update returned null for ID: {}", request.getId());
                responseObserver.onError(io.grpc.Status.INTERNAL.withDescription("Update failed").asRuntimeException());
                return;
            }

            responseObserver.onNext(mapToProto(updatedUser));
            responseObserver.onCompleted();
        } catch (io.grpc.StatusRuntimeException e) {
            responseObserver.onError(e);
        } catch (Exception e) {
            LOGGER.error("Error in updateUser for ID: {}", request.getId(), e);
            Throwable cause = e.getCause();
            if (cause instanceof io.grpc.StatusRuntimeException) {
                responseObserver.onError((io.grpc.StatusRuntimeException) cause);
            } else {
                responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
            }
        }
    }

    private UserResponse mapToProto(User user) {
        return UserResponse.newBuilder()
                .setId(user.getUid().toString())
                .setUsername(user.getUserName())
                .setEmail(user.getEmail())
                .setFirstName(user.getFirstName() == null ? "" : user.getFirstName())
                .setLastName(user.getLastName() == null ? "" : user.getLastName())
                .setAccountId(user.getAccountId() == null ? "" : user.getAccountId().toString())
                .setIsActive(user.getIsActive() != null && user.getIsActive())
                .addAllRoles(user.getRoles() == null ? Collections.emptyList() : user.getRoles())
                .build();
    }
}
