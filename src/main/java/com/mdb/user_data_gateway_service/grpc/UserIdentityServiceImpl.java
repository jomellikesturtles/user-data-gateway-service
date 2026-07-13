package com.mdb.user_data_gateway_service.grpc;

import com.mdb.user_data_gateway_service.entity.identity.Account;
import com.mdb.user_data_gateway_service.entity.identity.Profile;
import com.mdb.user_data_gateway_service.entity.identity.Preferences;
import com.mdb.user_data_gateway_service.repository.identity.AccountRepository;
import com.mdb.user_data_gateway_service.repository.identity.ProfileRepository;
import com.mdb.user_data_gateway_service.repository.identity.PreferencesRepository;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@GrpcService
public class UserIdentityServiceImpl extends UserIdentityServiceGrpc.UserIdentityServiceImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserIdentityServiceImpl.class);

    private final AccountRepository accountRepository;
    private final ProfileRepository profileRepository;
    private final PreferencesRepository preferencesRepository;

    public UserIdentityServiceImpl(AccountRepository accountRepository,
                                    ProfileRepository profileRepository,
                                    PreferencesRepository preferencesRepository) {
        this.accountRepository = accountRepository;
        this.profileRepository = profileRepository;
        this.preferencesRepository = preferencesRepository;
    }

    // --- ACCOUNTS ---

    @Override
    @Transactional("identityTransactionManager")
    public void createAccount(CreateAccountRequest request, StreamObserver<AccountResponse> responseObserver) {
        LOGGER.info("createAccount request received for email: '{}'", request.getEmail());
        try {
            Account existing = accountRepository.findByEmail(request.getEmail());
            if (existing != null) {
                LOGGER.warn("Account already exists with email: '{}'", request.getEmail());
                responseObserver.onError(io.grpc.Status.ALREADY_EXISTS
                        .withDescription("Account already exists with email: " + request.getEmail())
                        .asRuntimeException());
                return;
            }

            Account account = Account.builder()
                    .id(UUID.randomUUID())
                    .email(request.getEmail())
                    .status("ACTIVE")
                    .build();

            accountRepository.saveAndFlush(account);
            LOGGER.info("Successfully created account for email: '{}', ID: {}", request.getEmail(), account.getId());

            responseObserver.onNext(mapToProto(account));
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in createAccount for email: '{}'", request.getEmail(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional(value = "identityTransactionManager", readOnly = true)
    public void findAccountByEmail(FindAccountRequest request, StreamObserver<AccountResponse> responseObserver) {
        LOGGER.info("findAccountByEmail request received for email: '{}'", request.getEmail());
        try {
            Account account = accountRepository.findByEmail(request.getEmail());
            if (account == null) {
                LOGGER.warn("Account not found with email: '{}'", request.getEmail());
                responseObserver.onError(io.grpc.Status.NOT_FOUND
                        .withDescription("Account not found with email: " + request.getEmail())
                        .asRuntimeException());
                return;
            }

            responseObserver.onNext(mapToProto(account));
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in findAccountByEmail for email: '{}'", request.getEmail(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional(value = "identityTransactionManager", readOnly = true)
    public void findAccountById(FindAccountRequest request, StreamObserver<AccountResponse> responseObserver) {
        LOGGER.info("findAccountById request received for ID: '{}'", request.getId());
        try {
            UUID accountId = UUID.fromString(request.getId());
            Optional<Account> accountOpt = accountRepository.findById(accountId);
            if (accountOpt.isEmpty()) {
                LOGGER.warn("Account not found with ID: '{}'", request.getId());
                responseObserver.onError(io.grpc.Status.NOT_FOUND
                        .withDescription("Account not found with ID: " + request.getId())
                        .asRuntimeException());
                return;
            }

            responseObserver.onNext(mapToProto(accountOpt.get()));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid UUID format in findAccountById: '{}'", request.getId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid UUID: " + e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            LOGGER.error("Error in findAccountById for ID: '{}'", request.getId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // --- PROFILES ---

    @Override
    @Transactional("identityTransactionManager")
    public void createProfile(ProfileRequest request, StreamObserver<ProfileResponse> responseObserver) {
        LOGGER.info("createProfile request received for user: '{}', name: '{}'", request.getUserId(), request.getName());
        try {
            Optional<Profile> existing = profileRepository.findByUserIdAndName(request.getUserId(), request.getName());
            if (existing.isPresent()) {
                LOGGER.warn("Profile already exists with name '{}' for user '{}'", request.getName(), request.getUserId());
                responseObserver.onError(io.grpc.Status.ALREADY_EXISTS
                        .withDescription("Profile already exists with name: " + request.getName())
                        .asRuntimeException());
                return;
            }

            Profile profile = Profile.builder()
                    .userId(request.getUserId())
                    .name(request.getName())
                    .avatar(request.getAvatar())
                    .bio(request.getBio())
                    .isMain(request.getIsMain())
                    .build();

            profileRepository.saveAndFlush(profile);
            LOGGER.info("Successfully created profile with ID: {}", profile.getId());

            responseObserver.onNext(mapToProto(profile));
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in createProfile for user: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional("identityTransactionManager")
    public void updateProfile(ProfileRequest request, StreamObserver<ProfileResponse> responseObserver) {
        LOGGER.info("updateProfile request received for ID: '{}'", request.getId());
        try {
            Optional<Profile> profileOpt = profileRepository.findById(request.getId());
            if (profileOpt.isEmpty()) {
                LOGGER.warn("Profile not found with ID: '{}'", request.getId());
                responseObserver.onError(io.grpc.Status.NOT_FOUND
                        .withDescription("Profile not found with ID: " + request.getId())
                        .asRuntimeException());
                return;
            }

            Profile profile = profileOpt.get();
            if (!request.getName().isEmpty()) profile.setName(request.getName());
            if (!request.getAvatar().isEmpty()) profile.setAvatar(request.getAvatar());
            if (!request.getBio().isEmpty()) profile.setBio(request.getBio());
            profile.setIsMain(request.getIsMain());

            profileRepository.saveAndFlush(profile);
            LOGGER.info("Successfully updated profile ID: {}", profile.getId());

            responseObserver.onNext(mapToProto(profile));
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in updateProfile for ID: '{}'", request.getId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional(value = "identityTransactionManager", readOnly = true)
    public void getProfilesByUserId(ProfileRequest request, StreamObserver<ProfilesListResponse> responseObserver) {
        LOGGER.info("getProfilesByUserId request received for user: '{}'", request.getUserId());
        try {
            List<Profile> profiles = profileRepository.findByUserId(request.getUserId());
            List<ProfileResponse> protoProfiles = profiles.stream()
                    .map(this::mapToProto)
                    .collect(Collectors.toList());

            responseObserver.onNext(ProfilesListResponse.newBuilder().addAllProfiles(protoProfiles).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in getProfilesByUserId for user: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional(value = "identityTransactionManager", readOnly = true)
    public void getProfileByUserIdAndName(ProfileRequest request, StreamObserver<ProfileResponse> responseObserver) {
        LOGGER.info("getProfileByUserIdAndName request received for user: '{}', name: '{}'", request.getUserId(), request.getName());
        try {
            Optional<Profile> profileOpt = profileRepository.findByUserIdAndName(request.getUserId(), request.getName());
            if (profileOpt.isEmpty()) {
                LOGGER.warn("Profile not found for user: '{}', name: '{}'", request.getUserId(), request.getName());
                responseObserver.onError(io.grpc.Status.NOT_FOUND
                        .withDescription("Profile not found with name: " + request.getName())
                        .asRuntimeException());
                return;
            }

            responseObserver.onNext(mapToProto(profileOpt.get()));
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in getProfileByUserIdAndName for user: '{}', name: '{}'", request.getUserId(), request.getName(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional(value = "identityTransactionManager", readOnly = true)
    public void getProfileByUserIdAndIsMain(ProfileRequest request, StreamObserver<ProfileResponse> responseObserver) {
        LOGGER.info("getProfileByUserIdAndIsMain request received for user: '{}', isMain: {}", request.getUserId(), request.getIsMain());
        try {
            Optional<Profile> profileOpt = profileRepository.findByUserIdAndIsMain(request.getUserId(), request.getIsMain());
            if (profileOpt.isEmpty()) {
                LOGGER.warn("Profile not found for user: '{}', isMain: {}", request.getUserId(), request.getIsMain());
                responseObserver.onError(io.grpc.Status.NOT_FOUND
                        .withDescription("Main profile not found")
                        .asRuntimeException());
                return;
            }

            responseObserver.onNext(mapToProto(profileOpt.get()));
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in getProfileByUserIdAndIsMain for user: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional("identityTransactionManager")
    public void deleteProfileByUserIdAndName(ProfileDeleteRequest request, StreamObserver<IdentityActionResponse> responseObserver) {
        LOGGER.info("deleteProfileByUserIdAndName request received for user: '{}', name: '{}'", request.getUserId(), request.getProfileName());
        try {
            profileRepository.deleteByUserIdAndName(request.getUserId(), request.getProfileName());
            LOGGER.info("Successfully deleted profile '{}' for user '{}'", request.getProfileName(), request.getUserId());

            responseObserver.onNext(IdentityActionResponse.newBuilder().setSuccess(true).setMessage("Profile deleted").build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in deleteProfileByUserIdAndName for user: '{}', name: '{}'", request.getUserId(), request.getProfileName(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // --- PREFERENCES ---

    @Override
    @Transactional(value = "identityTransactionManager", readOnly = true)
    public void getPreferencesByUserId(PreferencesRequest request, StreamObserver<PreferencesResponse> responseObserver) {
        LOGGER.info("getPreferencesByUserId request received for user: '{}'", request.getUserId());
        try {
            Optional<Preferences> prefsOpt = preferencesRepository.findByUserId(request.getUserId());
            if (prefsOpt.isEmpty()) {
                LOGGER.info("No preferences found for user: '{}', returning empty default", request.getUserId());
                // Create a transient default preference model to return
                Preferences defaultPrefs = Preferences.builder()
                        .userId(request.getUserId())
                        .language("en")
                        .isDarkMode(false)
                        .isAutoScanEnabled(false)
                        .autoPlayTrailer(true)
                        .playTrailerBeforeShow(false)
                        .isFavoritesPrivate(false)
                        .isBookmarksPrivate(false)
                        .isPlayedPrivate(false)
                        .isProgressPrivate(false)
                        .build();
                responseObserver.onNext(mapToProto(defaultPrefs));
                responseObserver.onCompleted();
                return;
            }

            responseObserver.onNext(mapToProto(prefsOpt.get()));
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in getPreferencesByUserId for user: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional("identityTransactionManager")
    public void savePreferences(PreferencesRequest request, StreamObserver<PreferencesResponse> responseObserver) {
        LOGGER.info("savePreferences request received for user: '{}'", request.getUserId());
        try {
            Optional<Preferences> prefsOpt = preferencesRepository.findByUserId(request.getUserId());
            Preferences prefs;

            if (prefsOpt.isPresent()) {
                prefs = prefsOpt.get();
                if (!request.getLanguage().isEmpty()) prefs.setLanguage(request.getLanguage());
                prefs.setIsDarkMode(request.getIsDarkMode());
                prefs.setIsAutoScanEnabled(request.getIsAutoScanEnabled());
                prefs.setTorrentSeedRatio(request.getTorrentSeedRatio());
                prefs.setAutoPlayTrailer(request.getAutoPlayTrailer());
                prefs.setPlayTrailerBeforeShow(request.getPlayTrailerBeforeShow());
                prefs.setIsFavoritesPrivate(request.getIsFavoritesPrivate());
                prefs.setIsBookmarksPrivate(request.getIsBookmarksPrivate());
                prefs.setIsPlayedPrivate(request.getIsPlayedPrivate());
                prefs.setIsProgressPrivate(request.getIsProgressPrivate());
            } else {
                prefs = Preferences.builder()
                        .userId(request.getUserId())
                        .language(request.getLanguage().isEmpty() ? "en" : request.getLanguage())
                        .isDarkMode(request.getIsDarkMode())
                        .isAutoScanEnabled(request.getIsAutoScanEnabled())
                        .torrentSeedRatio(request.getTorrentSeedRatio())
                        .autoPlayTrailer(request.getAutoPlayTrailer())
                        .playTrailerBeforeShow(request.getPlayTrailerBeforeShow())
                        .isFavoritesPrivate(request.getIsFavoritesPrivate())
                        .isBookmarksPrivate(request.getIsBookmarksPrivate())
                        .isPlayedPrivate(request.getIsPlayedPrivate())
                        .isProgressPrivate(request.getIsProgressPrivate())
                        .build();
            }

            preferencesRepository.saveAndFlush(prefs);
            LOGGER.info("Successfully saved preferences for user: '{}'", request.getUserId());

            responseObserver.onNext(mapToProto(prefs));
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in savePreferences for user: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional("identityTransactionManager")
    public void deletePreferencesByUserId(PreferencesRequest request, StreamObserver<IdentityActionResponse> responseObserver) {
        LOGGER.info("deletePreferencesByUserId request received for user: '{}'", request.getUserId());
        try {
            preferencesRepository.deleteByUserId(request.getUserId());
            LOGGER.info("Successfully deleted preferences for user: '{}'", request.getUserId());

            responseObserver.onNext(IdentityActionResponse.newBuilder().setSuccess(true).setMessage("Preferences deleted").build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in deletePreferencesByUserId for user: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // --- MAPPING HELPERS ---

    private AccountResponse mapToProto(Account account) {
        long epoch = account.getCreatedAt() != null ? account.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli() : 0;
        return AccountResponse.newBuilder()
                .setId(account.getId() != null ? account.getId().toString() : "")
                .setEmail(account.getEmail() != null ? account.getEmail() : "")
                .setStatus(account.getStatus() != null ? account.getStatus() : "")
                .setCreatedAt(epoch)
                .build();
    }

    private ProfileResponse mapToProto(Profile profile) {
        return ProfileResponse.newBuilder()
                .setId(profile.getId() != null ? profile.getId() : "")
                .setUserId(profile.getUserId() != null ? profile.getUserId() : "")
                .setName(profile.getName() != null ? profile.getName() : "")
                .setAvatar(profile.getAvatar() != null ? profile.getAvatar() : "")
                .setBio(profile.getBio() != null ? profile.getBio() : "")
                .setIsMain(profile.getIsMain() != null && profile.getIsMain())
                .build();
    }

    private PreferencesResponse mapToProto(Preferences prefs) {
        return PreferencesResponse.newBuilder()
                .setId(prefs.getId() != null ? prefs.getId().toString() : "")
                .setUserId(prefs.getUserId() != null ? prefs.getUserId() : "")
                .setLanguage(prefs.getLanguage() != null ? prefs.getLanguage() : "en")
                .setIsDarkMode(prefs.getIsDarkMode() != null && prefs.getIsDarkMode())
                .setIsAutoScanEnabled(prefs.getIsAutoScanEnabled() != null && prefs.getIsAutoScanEnabled())
                .setTorrentSeedRatio(prefs.getTorrentSeedRatio() != null ? prefs.getTorrentSeedRatio() : 0.0)
                .setAutoPlayTrailer(prefs.getAutoPlayTrailer() == null || prefs.getAutoPlayTrailer())
                .setPlayTrailerBeforeShow(prefs.getPlayTrailerBeforeShow() != null && prefs.getPlayTrailerBeforeShow())
                .setIsFavoritesPrivate(prefs.getIsFavoritesPrivate() != null && prefs.getIsFavoritesPrivate())
                .setIsBookmarksPrivate(prefs.getIsBookmarksPrivate() != null && prefs.getIsBookmarksPrivate())
                .setIsPlayedPrivate(prefs.getIsPlayedPrivate() != null && prefs.getIsPlayedPrivate())
                .setIsProgressPrivate(prefs.getIsProgressPrivate() != null && prefs.getIsProgressPrivate())
                .build();
    }
}
