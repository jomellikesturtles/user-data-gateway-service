package com.mdb.user_data_gateway_service.grpc;

import com.mdb.user_data_gateway_service.entity.interaction.AdminUser;
import com.mdb.user_data_gateway_service.repository.interaction.AdminUserRepository;
import com.mdb.user_data_gateway_service.utils.PasswordUtils;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZoneOffset;
import java.util.UUID;
import java.util.stream.Collectors;

@GrpcService
public class AdminUserServiceImpl extends AdminUserServiceGrpc.AdminUserServiceImplBase {

    private final AdminUserRepository adminUserRepository;
    private final TransactionTemplate transactionTemplate;
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserServiceImpl.class);

    public AdminUserServiceImpl(AdminUserRepository adminUserRepository,
                                @Qualifier("interactionTransactionTemplate") TransactionTemplate transactionTemplate) {
        this.adminUserRepository = adminUserRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void registerAdmin(AdminUserRegisterRequest request, StreamObserver<AdminUserRegisterResponse> responseObserver) {
        LOGGER.info("RegisterAdmin request received for username: '{}', email: '{}'", request.getUsername(), request.getEmail());
        try {
            // Check if user already exists
            if (adminUserRepository.findByUsernameOrEmail(request.getUsername(), request.getEmail()).isPresent()) {
                LOGGER.warn("Registration failed: Username '{}' or Email '{}' already exists", request.getUsername(), request.getEmail());
                responseObserver.onNext(AdminUserRegisterResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Username or Email already exists")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            AdminUser registeredAdmin = transactionTemplate.execute(status -> {
                try {
                    String hashedPassword = PasswordUtils.getSaltedHash(request.getPassword());
                    AdminUser admin = AdminUser.builder()
                            .id(UUID.randomUUID())
                            .username(request.getUsername())
                            .email(request.getEmail())
                            .password(hashedPassword)
                            .status("ACTIVE")
                            .build();
                    return adminUserRepository.saveAndFlush(admin);
                } catch (Exception e) {
                    LOGGER.error("Failed to save admin user inside transaction", e);
                    throw new RuntimeException(e);
                }
            });

            if (registeredAdmin == null) {
                responseObserver.onNext(AdminUserRegisterResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Failed to register admin")
                        .build());
            } else {
                responseObserver.onNext(AdminUserRegisterResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Admin registered successfully")
                        .setId(registeredAdmin.getId().toString())
                        .build());
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in registerAdmin", e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void loginAdmin(AdminUserLoginRequest request, StreamObserver<AdminUserResponse> responseObserver) {
        LOGGER.info("LoginAdmin request received for identifier: '{}'", request.getUsernameOrEmail());
        try {
            AdminUser admin = adminUserRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                    .orElse(null);

            if (admin == null || !PasswordUtils.check(request.getPassword(), admin.getPassword())) {
                LOGGER.warn("Login failed: invalid credentials for identifier '{}'", request.getUsernameOrEmail());
                responseObserver.onError(io.grpc.Status.UNAUTHENTICATED.withDescription("Invalid credentials").asRuntimeException());
                return;
            }

            responseObserver.onNext(mapToProto(admin));
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in loginAdmin", e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getAdminUsers(GetAdminUsersRequest request, StreamObserver<GetAdminUsersResponse> responseObserver) {
        LOGGER.info("GetAdminUsers request received (page: {}, size: {})", request.getPage(), request.getSize());
        try {
            int page = Math.max(0, request.getPage());
            int size = request.getSize() <= 0 ? 10 : request.getSize();

            Page<AdminUser> adminPage = adminUserRepository.findAll(PageRequest.of(page, size));

            GetAdminUsersResponse response = GetAdminUsersResponse.newBuilder()
                    .addAllUsers(adminPage.getContent().stream()
                            .map(this::mapToProto)
                            .collect(Collectors.toList()))
                    .setTotalElements(adminPage.getTotalElements())
                    .setTotalPages(adminPage.getTotalPages())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in getAdminUsers", e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    private AdminUserResponse mapToProto(AdminUser admin) {
        long createdAtSeconds = admin.getCreatedAt() != null 
                ? admin.getCreatedAt().toEpochSecond(ZoneOffset.UTC) 
                : 0L;
        long updatedAtSeconds = admin.getUpdatedAt() != null 
                ? admin.getUpdatedAt().toEpochSecond(ZoneOffset.UTC) 
                : 0L;

        return AdminUserResponse.newBuilder()
                .setId(admin.getId().toString())
                .setUsername(admin.getUsername())
                .setEmail(admin.getEmail())
                .setStatus(admin.getStatus() == null ? "" : admin.getStatus())
                .setCreatedAt(createdAtSeconds)
                .setUpdatedAt(updatedAtSeconds)
                .build();
    }
}
