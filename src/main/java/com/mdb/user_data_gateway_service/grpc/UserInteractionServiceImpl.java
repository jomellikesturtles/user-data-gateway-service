package com.mdb.user_data_gateway_service.grpc;

import com.mdb.user_data_gateway_service.entity.*;
import com.mdb.user_data_gateway_service.repository.*;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@GrpcService
public class UserInteractionServiceImpl extends UserInteractionServiceGrpc.UserInteractionServiceImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserInteractionServiceImpl.class);

    private final BookmarkRepository bookmarkRepository;
    private final FavoriteRepository favoriteRepository;
    private final PlaybackProgressRepository progressRepository;
    private final ProfileRepository profileRepository;
    private final ReviewRepository reviewRepository;
    private final MediaListRepository mediaListRepository;
    private final ListLinkMovieRepository listLinkMovieRepository;
    private final PlayedRepository playedRepository;

    public UserInteractionServiceImpl(BookmarkRepository bookmarkRepository,
                                       FavoriteRepository favoriteRepository,
                                       PlaybackProgressRepository progressRepository,
                                       ProfileRepository profileRepository,
                                       ReviewRepository reviewRepository,
                                       MediaListRepository mediaListRepository,
                                       ListLinkMovieRepository listLinkMovieRepository,
                                       PlayedRepository playedRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.favoriteRepository = favoriteRepository;
        this.progressRepository = progressRepository;
        this.profileRepository = profileRepository;
        this.reviewRepository = reviewRepository;
        this.mediaListRepository = mediaListRepository;
        this.listLinkMovieRepository = listLinkMovieRepository;
        this.playedRepository = playedRepository;
    }

    // --- BOOKMARKS ---

    @Override
    @Transactional
    public void addBookmark(InteractionRequest request, StreamObserver<InteractionActionResponse> responseObserver) {
        LOGGER.info("addBookmark request received for user: '{}', media: '{}'", request.getUserId(), request.getMediaId());
        try {
            UUID.fromString(request.getUserId());
            String userId = request.getUserId();
            String mediaId = request.getMediaId();

            String profileId = getOrCreateMainProfileId(userId);

            if (bookmarkRepository.existsByUserIdAndMediaId(userId, mediaId)) {
                LOGGER.info("Bookmark already exists for user: '{}', media: '{}'", userId, mediaId);
                responseObserver.onNext(InteractionActionResponse.newBuilder().setSuccess(true).setMessage("Already bookmarked").build());
                responseObserver.onCompleted();
                return;
            }

            Bookmark bookmark = Bookmark.builder()
                    .profileId(profileId)
                    .userId(userId)
                    .mediaId(mediaId)
                    .tmdbId(mediaId)
                    .build();
            bookmarkRepository.saveAndFlush(bookmark);
            LOGGER.info("Successfully added bookmark for user: '{}', media: '{}'", userId, mediaId);

            responseObserver.onNext(InteractionActionResponse.newBuilder().setSuccess(true).setMessage("Bookmark added").build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid UUID format in addBookmark: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid UUID: " + e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            LOGGER.error("Error in addBookmark for user: '{}', media: '{}'", request.getUserId(), request.getMediaId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional
    public void removeBookmark(InteractionRequest request, StreamObserver<InteractionActionResponse> responseObserver) {
        LOGGER.info("removeBookmark request received for user: '{}', media: '{}'", request.getUserId(), request.getMediaId());
        try {
            UUID.fromString(request.getUserId());
            String userId = request.getUserId();
            String mediaId = request.getMediaId();

            bookmarkRepository.deleteByUserIdAndMediaId(userId, mediaId);
            LOGGER.info("Successfully removed bookmark for user: '{}', media: '{}'", userId, mediaId);

            responseObserver.onNext(InteractionActionResponse.newBuilder().setSuccess(true).setMessage("Bookmark removed").build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid UUID format in removeBookmark: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid UUID: " + e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            LOGGER.error("Error in removeBookmark for user: '{}', media: '{}'", request.getUserId(), request.getMediaId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getBookmarks(InteractionRequest request, StreamObserver<BookmarksListResponse> responseObserver) {
        LOGGER.info("getBookmarks request received for user: '{}', page: {}, size: {}", request.getUserId(), request.getPage(), request.getSize());
        try {
            UUID.fromString(request.getUserId());
            String userId = request.getUserId();
            int page = Math.max(0, request.getPage());
            int size = request.getSize() > 0 ? request.getSize() : 20;

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            List<Bookmark> bookmarks = bookmarkRepository.findByUserId(userId, pageable);
            long total = bookmarkRepository.countByUserId(userId);

            List<BookmarkResponse> bookmarkResponses = bookmarks.stream()
                    .map(b -> BookmarkResponse.newBuilder()
                            .setId(b.getId().toString())
                            .setUserId(b.getUserId())
                            .setMediaId(b.getMediaId())
                            .setCreatedAt(b.getCreatedAt() != null ? b.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0)
                            .build())
                    .collect(Collectors.toList());

            BookmarksListResponse res = BookmarksListResponse.newBuilder()
                    .addAllBookmarks(bookmarkResponses)
                    .setTotalElements(total)
                    .build();

            responseObserver.onNext(res);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid UUID format in getBookmarks: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid UUID: " + e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            LOGGER.error("Error in getBookmarks for user: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void isBookmarked(InteractionRequest request, StreamObserver<InteractionActionResponse> responseObserver) {
        LOGGER.info("isBookmarked request received for user: '{}', media: '{}'", request.getUserId(), request.getMediaId());
        try {
            UUID.fromString(request.getUserId());
            String userId = request.getUserId();
            String mediaId = request.getMediaId();

            boolean exists = bookmarkRepository.existsByUserIdAndMediaId(userId, mediaId);
            responseObserver.onNext(InteractionActionResponse.newBuilder().setSuccess(exists).setMessage(exists ? "Bookmarked" : "Not bookmarked").build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid UUID format in isBookmarked: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid UUID: " + e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            LOGGER.error("Error in isBookmarked for user: '{}', media: '{}'", request.getUserId(), request.getMediaId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // --- FAVORITES ---

    @Override
    @Transactional
    public void addFavorite(InteractionRequest request, StreamObserver<InteractionActionResponse> responseObserver) {
        LOGGER.info("addFavorite request received for user: '{}', media: '{}'", request.getUserId(), request.getMediaId());
        try {
            UUID.fromString(request.getUserId());
            String userId = request.getUserId();
            String mediaId = request.getMediaId();

            String profileId = getOrCreateMainProfileId(userId);

            if (favoriteRepository.existsByUserIdAndMediaId(userId, mediaId)) {
                LOGGER.info("Favorite already exists for user: '{}', media: '{}'", userId, mediaId);
                responseObserver.onNext(InteractionActionResponse.newBuilder().setSuccess(true).setMessage("Already favorited").build());
                responseObserver.onCompleted();
                return;
            }

            Favorite favorite = Favorite.builder()
                    .profileId(profileId)
                    .userId(userId)
                    .mediaId(mediaId)
                    .tmdbId(mediaId)
                    .build();
            favoriteRepository.saveAndFlush(favorite);
            LOGGER.info("Successfully added favorite for user: '{}', media: '{}'", userId, mediaId);

            responseObserver.onNext(InteractionActionResponse.newBuilder().setSuccess(true).setMessage("Favorite added").build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid UUID format in addFavorite: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid UUID: " + e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            LOGGER.error("Error in addFavorite for user: '{}', media: '{}'", request.getUserId(), request.getMediaId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional
    public void removeFavorite(InteractionRequest request, StreamObserver<InteractionActionResponse> responseObserver) {
        LOGGER.info("removeFavorite request received for user: '{}', media: '{}'", request.getUserId(), request.getMediaId());
        try {
            UUID.fromString(request.getUserId());
            String userId = request.getUserId();
            String mediaId = request.getMediaId();

            favoriteRepository.deleteByUserIdAndMediaId(userId, mediaId);
            LOGGER.info("Successfully removed favorite for user: '{}', media: '{}'", userId, mediaId);

            responseObserver.onNext(InteractionActionResponse.newBuilder().setSuccess(true).setMessage("Favorite removed").build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid UUID format in removeFavorite: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid UUID: " + e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            LOGGER.error("Error in removeFavorite for user: '{}', media: '{}'", request.getUserId(), request.getMediaId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getFavorites(InteractionRequest request, StreamObserver<FavoritesListResponse> responseObserver) {
        LOGGER.info("getFavorites request received for user: '{}', page: {}, size: {}", request.getUserId(), request.getPage(), request.getSize());
        try {
            UUID.fromString(request.getUserId());
            String userId = request.getUserId();
            int page = Math.max(0, request.getPage());
            int size = request.getSize() > 0 ? request.getSize() : 20;

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            List<Favorite> favorites = favoriteRepository.findByUserId(userId, pageable);
            long total = favoriteRepository.countByUserId(userId);

            List<FavoriteResponse> favoriteResponses = favorites.stream()
                    .map(f -> FavoriteResponse.newBuilder()
                            .setId(f.getId().toString())
                            .setUserId(f.getUserId())
                            .setMediaId(f.getMediaId())
                            .setCreatedAt(f.getCreatedAt() != null ? f.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0)
                            .build())
                    .collect(Collectors.toList());

            FavoritesListResponse res = FavoritesListResponse.newBuilder()
                    .addAllFavorites(favoriteResponses)
                    .setTotalElements(total)
                    .build();

            responseObserver.onNext(res);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid UUID format in getFavorites: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid UUID: " + e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            LOGGER.error("Error in getFavorites for user: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void isFavorite(InteractionRequest request, StreamObserver<InteractionActionResponse> responseObserver) {
        LOGGER.info("isFavorite request received for user: '{}', media: '{}'", request.getUserId(), request.getMediaId());
        try {
            UUID.fromString(request.getUserId());
            String userId = request.getUserId();
            String mediaId = request.getMediaId();

            boolean exists = favoriteRepository.existsByUserIdAndMediaId(userId, mediaId);
            responseObserver.onNext(InteractionActionResponse.newBuilder().setSuccess(exists).setMessage(exists ? "Favorite" : "Not favorite").build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid UUID format in isFavorite: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid UUID: " + e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            LOGGER.error("Error in isFavorite for user: '{}', media: '{}'", request.getUserId(), request.getMediaId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // --- PLAYBACK PROGRESS ---

    @Override
    @Transactional
    public void savePlaybackProgress(ProgressRequest request, StreamObserver<ProgressResponse> responseObserver) {
        LOGGER.info("savePlaybackProgress request received for user: '{}', media: '{}', progress: {}s/{}s, pct: {}%",
                request.getUserId(), request.getMediaId(), request.getProgressSeconds(), request.getTotalSeconds(), request.getPercentage());
        try {
            UUID.fromString(request.getUserId());
            String userId = request.getUserId();
            String mediaId = request.getMediaId();

            String profileId = getOrCreateMainProfileId(userId);
            Optional<PlaybackProgress> progressOpt = progressRepository.findByProfileIdAndMediaId(profileId, mediaId);
            PlaybackProgress progress;

            double percentage = request.getPercentage() > 0 ? request.getPercentage() :
                    (request.getTotalSeconds() > 0 ? ((double) request.getProgressSeconds() * 100.0 / request.getTotalSeconds()) : 0.0);
            percentage = Math.min(100.0, percentage);

            boolean isFinished = percentage >= 99.0;

            if (progressOpt.isPresent()) {
                progress = progressOpt.get();
                progress.setPercentage(percentage);
            } else {
                progress = PlaybackProgress.builder()
                        .profileId(profileId)
                        .userId(userId)
                        .mediaId(mediaId)
                        .tmdbId(mediaId)
                        .percentage(percentage)
                        .build();
            }
            progressRepository.saveAndFlush(progress);

            if (isFinished) {
                Optional<Played> playedOpt = playedRepository.findByProfileIdAndMediaId(profileId, mediaId);
                if (playedOpt.isEmpty()) {
                    Played played = Played.builder()
                            .profileId(profileId)
                            .userId(userId)
                            .mediaId(mediaId)
                            .tmdbId(mediaId)
                            .percentage(percentage)
                            .build();
                    playedRepository.saveAndFlush(played);
                } else {
                    Played played = playedOpt.get();
                    played.setPercentage(percentage);
                    playedRepository.saveAndFlush(played);
                }
            }

            LOGGER.info("Successfully saved progress for user: '{}', media: '{}', percentage: {}%, isFinished: {}", userId, mediaId, percentage, isFinished);

            ProgressResponse res = ProgressResponse.newBuilder()
                    .setId(progress.getId())
                    .setUserId(progress.getUserId())
                    .setMediaId(progress.getMediaId())
                    .setProgressSeconds(request.getProgressSeconds())
                    .setTotalSeconds(request.getTotalSeconds())
                    .setIsFinished(isFinished)
                    .setPercentage(percentage)
                    .build();
            responseObserver.onNext(res);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid UUID format in savePlaybackProgress: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid UUID: " + e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            LOGGER.error("Error in savePlaybackProgress for user: '{}', media: '{}'", request.getUserId(), request.getMediaId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getPlaybackProgress(InteractionRequest request, StreamObserver<ProgressResponse> responseObserver) {
        LOGGER.info("getPlaybackProgress request received for user: '{}', media: '{}'", request.getUserId(), request.getMediaId());
        try {
            UUID.fromString(request.getUserId());
            String userId = request.getUserId();
            String mediaId = request.getMediaId();

            Optional<String> profileIdOpt = getMainProfileId(userId);
            Optional<PlaybackProgress> progressOpt = profileIdOpt.flatMap(profileId -> progressRepository.findByProfileIdAndMediaId(profileId, mediaId));
            if (progressOpt.isEmpty()) {
                LOGGER.info("No playback progress found for user: '{}', media: '{}'", userId, mediaId);
                responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Playback progress not found").asRuntimeException());
                return;
            }

            PlaybackProgress progress = progressOpt.get();
            double percentage = progress.getPercentage() != null ? progress.getPercentage() : 0.0;
            long progressSeconds = (long) (percentage * 100.0);
            long totalSeconds = 10000L;
            boolean isFinished = percentage >= 99.0;

            ProgressResponse res = ProgressResponse.newBuilder()
                    .setId(progress.getId())
                    .setUserId(progress.getUserId())
                    .setMediaId(progress.getMediaId())
                    .setProgressSeconds(progressSeconds)
                    .setTotalSeconds(totalSeconds)
                    .setIsFinished(isFinished)
                    .setPercentage(percentage)
                    .build();
            responseObserver.onNext(res);
            responseObserver.onCompleted();
            LOGGER.info("Successfully retrieved progress for user: '{}', media: '{}'", userId, mediaId);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid UUID format in getPlaybackProgress: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid UUID: " + e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            LOGGER.error("Error in getPlaybackProgress for user: '{}', media: '{}'", request.getUserId(), request.getMediaId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getPlayedList(InteractionRequest request, StreamObserver<ProgressListResponse> responseObserver) {
        LOGGER.info("getPlayedList request received for user: '{}', page: {}, size: {}", request.getUserId(), request.getPage(), request.getSize());
        try {
            UUID.fromString(request.getUserId());
            String userId = request.getUserId();
            int page = Math.max(0, request.getPage());
            int size = request.getSize() > 0 ? request.getSize() : 20;

            String profileId = getOrCreateMainProfileId(userId);
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            List<Played> playedList = playedRepository.findByProfileId(profileId, pageable);
            long total = playedRepository.countByProfileId(profileId);

            List<ProgressResponse> progressResponses = playedList.stream()
                    .map(p -> ProgressResponse.newBuilder()
                            .setId(p.getId().toString())
                            .setUserId(p.getUserId())
                            .setMediaId(p.getMediaId())
                            .setProgressSeconds((long) (p.getPercentage() * 100.0))
                            .setTotalSeconds(10000L)
                            .setIsFinished(true)
                            .setPercentage(p.getPercentage())
                            .build())
                    .collect(Collectors.toList());

            ProgressListResponse res = ProgressListResponse.newBuilder()
                    .addAllProgresses(progressResponses)
                    .setTotalElements(total)
                    .build();

            responseObserver.onNext(res);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid UUID format in getPlayedList: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid UUID: " + e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            LOGGER.error("Error in getPlayedList for user: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getProgressList(InteractionRequest request, StreamObserver<ProgressListResponse> responseObserver) {
        LOGGER.info("getProgressList request received for user: '{}', page: {}, size: {}", request.getUserId(), request.getPage(), request.getSize());
        try {
            UUID.fromString(request.getUserId());
            String userId = request.getUserId();
            int page = Math.max(0, request.getPage());
            int size = request.getSize() > 0 ? request.getSize() : 20;

            String profileId = getOrCreateMainProfileId(userId);
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            List<PlaybackProgress> progresses = progressRepository.findByProfileId(profileId, pageable);
            long total = progressRepository.countByProfileId(profileId);

            List<ProgressResponse> progressResponses = progresses.stream()
                    .map(p -> ProgressResponse.newBuilder()
                            .setId(p.getId())
                            .setUserId(p.getUserId())
                            .setMediaId(p.getMediaId())
                            .setProgressSeconds((long) (p.getPercentage() * 100.0))
                            .setTotalSeconds(10000L)
                            .setIsFinished(p.getPercentage() >= 99.0)
                            .setPercentage(p.getPercentage())
                            .build())
                    .collect(Collectors.toList());

            ProgressListResponse res = ProgressListResponse.newBuilder()
                    .addAllProgresses(progressResponses)
                    .setTotalElements(total)
                    .build();

            responseObserver.onNext(res);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid UUID format in getProgressList: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid UUID: " + e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            LOGGER.error("Error in getProgressList for user: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional
    public void deletePlaybackProgress(InteractionRequest request, StreamObserver<InteractionActionResponse> responseObserver) {
        LOGGER.info("deletePlaybackProgress request received for user: '{}', media: '{}'", request.getUserId(), request.getMediaId());
        try {
            UUID.fromString(request.getUserId());
            String userId = request.getUserId();
            String mediaId = request.getMediaId();

            String profileId = getOrCreateMainProfileId(userId);
            progressRepository.deleteByProfileIdAndMediaId(profileId, mediaId);
            playedRepository.deleteByProfileIdAndMediaId(profileId, mediaId);

            responseObserver.onNext(InteractionActionResponse.newBuilder().setSuccess(true).setMessage("Progress deleted").build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid UUID format in deletePlaybackProgress: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid UUID: " + e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            LOGGER.error("Error in deletePlaybackProgress for user: '{}', media: '{}'", request.getUserId(), request.getMediaId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // --- REVIEWS ---

    @Override
    @Transactional
    public void submitReview(ReviewRequest request, StreamObserver<ReviewResponse> responseObserver) {
        LOGGER.info("submitReview request received for user: '{}', media: '{}'", request.getUserId(), request.getMediaId());
        try {
            UUID.fromString(request.getUserId());
            String userId = request.getUserId();
            String mediaId = request.getMediaId();

            String profileId = getOrCreateMainProfileId(userId);
            Review review;

            if (request.getId() != null && !request.getId().isEmpty()) {
                Long id = Long.parseLong(request.getId());
                Optional<Review> existingOpt = reviewRepository.findById(id);
                if (existingOpt.isPresent()) {
                    review = existingOpt.get();
                    review.setContent(request.getContent());
                    review.setTitle(request.getTitle());
                    review.setRating(request.getRating());
                } else {
                    review = Review.builder()
                            .id(id)
                            .profileId(profileId)
                            .mediaId(mediaId)
                            .content(request.getContent())
                            .title(request.getTitle())
                            .rating(request.getRating())
                            .likesNum(0)
                            .dislikesNum(0)
                            .build();
                }
            } else {
                review = Review.builder()
                        .profileId(profileId)
                        .mediaId(mediaId)
                        .content(request.getContent())
                        .title(request.getTitle())
                        .rating(request.getRating())
                        .likesNum(0)
                        .dislikesNum(0)
                        .build();
            }

            reviewRepository.saveAndFlush(review);
            LOGGER.info("Successfully saved review for user '{}' media '{}' id: {}", userId, mediaId, review.getId());

            ReviewResponse res = ReviewResponse.newBuilder()
                    .setId(review.getId().toString())
                    .setUserId(userId)
                    .setMediaId(review.getMediaId())
                    .setContent(review.getContent() != null ? review.getContent() : "")
                    .setTitle(review.getTitle() != null ? review.getTitle() : "")
                    .setRating(review.getRating() != null ? review.getRating() : 0.0)
                    .setCreatedAt(review.getCreatedAt() != null ? review.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0)
                    .setUpdatedAt(review.getUpdatedAt() != null ? review.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0)
                    .build();

            responseObserver.onNext(res);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid UUID or ID format in submitReview: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid request: " + e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            LOGGER.error("Error in submitReview for user: '{}', media: '{}'", request.getUserId(), request.getMediaId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional
    public void deleteReview(ReviewRequest request, StreamObserver<InteractionActionResponse> responseObserver) {
        LOGGER.info("deleteReview request received for id: '{}', user: '{}'", request.getId(), request.getUserId());
        try {
            Long id = Long.parseLong(request.getId());
            reviewRepository.deleteById(id);
            LOGGER.info("Successfully deleted review with id: {}", id);

            responseObserver.onNext(InteractionActionResponse.newBuilder().setSuccess(true).setMessage("Review deleted").build());
            responseObserver.onCompleted();
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid ID format in deleteReview: '{}'", request.getId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid ID format: " + e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            LOGGER.error("Error in deleteReview for id: '{}'", request.getId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getReviewsByMedia(InteractionRequest request, StreamObserver<ReviewsListResponse> responseObserver) {
        LOGGER.info("getReviewsByMedia request received for media: '{}', page: {}, size: {}", request.getMediaId(), request.getPage(), request.getSize());
        try {
            String mediaId = request.getMediaId();
            int page = Math.max(0, request.getPage());
            int size = request.getSize() > 0 ? request.getSize() : 20;

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            List<Review> reviews = reviewRepository.findByMediaId(mediaId, pageable);
            long total = reviewRepository.countByMediaId(mediaId);

            List<ReviewResponse> reviewResponses = reviews.stream()
                    .map(r -> ReviewResponse.newBuilder()
                            .setId(r.getId().toString())
                            .setUserId(r.getProfileId())
                            .setMediaId(r.getMediaId())
                            .setContent(r.getContent() != null ? r.getContent() : "")
                            .setTitle(r.getTitle() != null ? r.getTitle() : "")
                            .setRating(r.getRating() != null ? r.getRating() : 0.0)
                            .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0)
                            .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0)
                            .build())
                    .collect(Collectors.toList());

            ReviewsListResponse res = ReviewsListResponse.newBuilder()
                    .addAllReviews(reviewResponses)
                    .setTotalElements(total)
                    .build();

            responseObserver.onNext(res);
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in getReviewsByMedia for media: '{}'", request.getMediaId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getReviewsByUser(InteractionRequest request, StreamObserver<ReviewsListResponse> responseObserver) {
        LOGGER.info("getReviewsByUser request received for user: '{}', page: {}, size: {}", request.getUserId(), request.getPage(), request.getSize());
        try {
            UUID.fromString(request.getUserId());
            String userId = request.getUserId();
            int page = Math.max(0, request.getPage());
            int size = request.getSize() > 0 ? request.getSize() : 20;

            String profileId = getOrCreateMainProfileId(userId);
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            List<Review> reviews = reviewRepository.findByProfileId(profileId, pageable);
            long total = reviewRepository.countByProfileId(profileId);

            List<ReviewResponse> reviewResponses = reviews.stream()
                    .map(r -> ReviewResponse.newBuilder()
                            .setId(r.getId().toString())
                            .setUserId(userId)
                            .setMediaId(r.getMediaId())
                            .setContent(r.getContent() != null ? r.getContent() : "")
                            .setTitle(r.getTitle() != null ? r.getTitle() : "")
                            .setRating(r.getRating() != null ? r.getRating() : 0.0)
                            .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0)
                            .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0)
                            .build())
                    .collect(Collectors.toList());

            ReviewsListResponse res = ReviewsListResponse.newBuilder()
                    .addAllReviews(reviewResponses)
                    .setTotalElements(total)
                    .build();

            responseObserver.onNext(res);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid UUID format in getReviewsByUser: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid UUID: " + e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            LOGGER.error("Error in getReviewsByUser for user: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // --- CUSTOM LISTS ---

    @Override
    @Transactional
    public void createList(ListRequest request, StreamObserver<ListResponse> responseObserver) {
        LOGGER.info("createList request received for user: '{}', name: '{}'", request.getUserId(), request.getName());
        try {
            UUID.fromString(request.getUserId());
            String userId = request.getUserId();
            String name = request.getName();

            String profileId = getOrCreateMainProfileId(userId);

            if (mediaListRepository.existsByUserIdAndName(userId, name)) {
                LOGGER.warn("List already exists with name '{}' for user '{}'", name, userId);
                responseObserver.onError(io.grpc.Status.ALREADY_EXISTS.withDescription("List already exists").asRuntimeException());
                return;
            }

            MediaList mediaList = MediaList.builder()
                    .name(name)
                    .description(request.getDescription())
                    .userId(userId)
                    .profileId(profileId)
                    .build();

            mediaListRepository.saveAndFlush(mediaList);
            LOGGER.info("Successfully created list with id: {}", mediaList.getId());

            ListResponse res = ListResponse.newBuilder()
                    .setId(mediaList.getId())
                    .setUserId(mediaList.getUserId())
                    .setName(mediaList.getName())
                    .setDescription(mediaList.getDescription() != null ? mediaList.getDescription() : "")
                    .setCreatedAt(mediaList.getCreatedAt() != null ? mediaList.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0)
                    .build();

            responseObserver.onNext(res);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid UUID format in createList: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid UUID: " + e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            LOGGER.error("Error in createList for user: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional
    public void deleteList(ListRequest request, StreamObserver<InteractionActionResponse> responseObserver) {
        LOGGER.info("deleteList request received for id: '{}', user: '{}'", request.getId(), request.getUserId());
        try {
            String listId = request.getId();
            listLinkMovieRepository.deleteByListId(listId);
            mediaListRepository.deleteById(listId);
            LOGGER.info("Successfully deleted list with id: {}", listId);

            responseObserver.onNext(InteractionActionResponse.newBuilder().setSuccess(true).setMessage("List deleted").build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in deleteList for id: '{}'", request.getId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional
    public void addItemToList(ListRequest request, StreamObserver<InteractionActionResponse> responseObserver) {
        LOGGER.info("addItemToList request received for list: '{}', media: '{}'", request.getId(), request.getMediaId());
        try {
            String listId = request.getId();
            String mediaId = request.getMediaId();
            String userId = request.getUserId();
            String profileId = getOrCreateMainProfileId(userId);

            if (listLinkMovieRepository.existsByListIdAndMediaId(listId, mediaId)) {
                LOGGER.info("Item already exists in list");
                responseObserver.onNext(InteractionActionResponse.newBuilder().setSuccess(true).setMessage("Item already in list").build());
                responseObserver.onCompleted();
                return;
            }

            ListLinkMovie link = ListLinkMovie.builder()
                    .listId(listId)
                    .userId(userId)
                    .profileId(profileId)
                    .mediaId(mediaId)
                    .tmdbId(mediaId)
                    .build();

            listLinkMovieRepository.saveAndFlush(link);
            LOGGER.info("Successfully added item to list");

            responseObserver.onNext(InteractionActionResponse.newBuilder().setSuccess(true).setMessage("Item added").build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in addItemToList", e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional
    public void removeItemFromList(ListRequest request, StreamObserver<InteractionActionResponse> responseObserver) {
        LOGGER.info("removeItemFromList request received for list: '{}', media: '{}'", request.getId(), request.getMediaId());
        try {
            String listId = request.getId();
            String mediaId = request.getMediaId();

            listLinkMovieRepository.deleteByListIdAndMediaId(listId, mediaId);
            LOGGER.info("Successfully removed item from list");

            responseObserver.onNext(InteractionActionResponse.newBuilder().setSuccess(true).setMessage("Item removed").build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in removeItemFromList", e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional
    public void editList(ListRequest request, StreamObserver<ListResponse> responseObserver) {
        LOGGER.info("editList request received for id: '{}'", request.getId());
        try {
            String listId = request.getId();
            Optional<MediaList> listOpt = mediaListRepository.findById(listId);
            if (listOpt.isEmpty()) {
                responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("List not found").asRuntimeException());
                return;
            }

            MediaList mediaList = listOpt.get();
            if (request.getName() != null && !request.getName().isEmpty()) {
                mediaList.setName(request.getName());
            }
            if (request.getDescription() != null) {
                mediaList.setDescription(request.getDescription());
            }

            mediaListRepository.saveAndFlush(mediaList);
            LOGGER.info("Successfully edited list with id: {}", listId);

            List<ListLinkMovie> links = listLinkMovieRepository.findByListId(listId);
            List<ListLinkMovieResponse> contentResponses = links.stream()
                    .map(link -> ListLinkMovieResponse.newBuilder()
                            .setId(link.getId())
                            .setListId(link.getListId())
                            .setUserId(link.getUserId())
                            .setMediaId(link.getMediaId())
                            .build())
                    .collect(Collectors.toList());

            ListResponse res = ListResponse.newBuilder()
                    .setId(mediaList.getId())
                    .setUserId(mediaList.getUserId())
                    .setName(mediaList.getName())
                    .setDescription(mediaList.getDescription() != null ? mediaList.getDescription() : "")
                    .addAllContents(contentResponses)
                    .setCreatedAt(mediaList.getCreatedAt() != null ? mediaList.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0)
                    .build();

            responseObserver.onNext(res);
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in editList for id: '{}'", request.getId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getSingleList(ListRequest request, StreamObserver<ListResponse> responseObserver) {
        LOGGER.info("getSingleList request received for id: '{}'", request.getId());
        try {
            String listId = request.getId();
            Optional<MediaList> listOpt = mediaListRepository.findById(listId);
            if (listOpt.isEmpty()) {
                responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("List not found").asRuntimeException());
                return;
            }

            MediaList mediaList = listOpt.get();
            List<ListLinkMovie> links = listLinkMovieRepository.findByListId(listId);
            List<ListLinkMovieResponse> contentResponses = links.stream()
                    .map(link -> ListLinkMovieResponse.newBuilder()
                            .setId(link.getId())
                            .setListId(link.getListId())
                            .setUserId(link.getUserId())
                            .setMediaId(link.getMediaId())
                            .build())
                    .collect(Collectors.toList());

            ListResponse res = ListResponse.newBuilder()
                    .setId(mediaList.getId())
                    .setUserId(mediaList.getUserId())
                    .setName(mediaList.getName())
                    .setDescription(mediaList.getDescription() != null ? mediaList.getDescription() : "")
                    .addAllContents(contentResponses)
                    .setCreatedAt(mediaList.getCreatedAt() != null ? mediaList.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0)
                    .build();

            responseObserver.onNext(res);
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Error in getSingleList for id: '{}'", request.getId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getLists(ListRequest request, StreamObserver<ListsListResponse> responseObserver) {
        LOGGER.info("getLists request received for user: '{}', page: {}, size: {}", request.getUserId(), request.getPage(), request.getSize());
        try {
            UUID.fromString(request.getUserId());
            String userId = request.getUserId();
            int page = Math.max(0, request.getPage());
            int size = request.getSize() > 0 ? request.getSize() : 20;

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            List<MediaList> lists = mediaListRepository.findByUserId(userId, pageable);
            long total = mediaListRepository.countByUserId(userId);

            List<ListResponse> listResponses = lists.stream()
                    .map(mediaList -> {
                        List<ListLinkMovie> links = listLinkMovieRepository.findByListId(mediaList.getId());
                        List<ListLinkMovieResponse> contentResponses = links.stream()
                                .map(link -> ListLinkMovieResponse.newBuilder()
                                        .setId(link.getId())
                                        .setListId(link.getListId())
                                        .setUserId(link.getUserId())
                                        .setMediaId(link.getMediaId())
                                        .build())
                                .collect(Collectors.toList());

                        return ListResponse.newBuilder()
                                .setId(mediaList.getId())
                                .setUserId(mediaList.getUserId())
                                .setName(mediaList.getName())
                                .setDescription(mediaList.getDescription() != null ? mediaList.getDescription() : "")
                                .addAllContents(contentResponses)
                                .setCreatedAt(mediaList.getCreatedAt() != null ? mediaList.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : 0)
                                .build();
                    })
                    .collect(Collectors.toList());

            ListsListResponse res = ListsListResponse.newBuilder()
                    .addAllLists(listResponses)
                    .setTotalElements(total)
                    .build();

            responseObserver.onNext(res);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid UUID format in getLists: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription("Invalid UUID: " + e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            LOGGER.error("Error in getLists for user: '{}'", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // --- HELPER METHODS ---

    private String getOrCreateMainProfileId(String userId) {
        return profileRepository.findByUserIdAndIsMain(userId, true)
                .map(Profile::getId)
                .orElseGet(() -> {
                    LOGGER.info("No main profile found for user {}. Creating default main profile.", userId);
                    Profile profile = Profile.builder()
                            .userId(userId)
                            .name("Main")
                            .avatar("default")
                            .bio("")
                            .isMain(true)
                            .build();
                    return profileRepository.saveAndFlush(profile).getId();
                });
    }

    private Optional<String> getMainProfileId(String userId) {
        return profileRepository.findByUserIdAndIsMain(userId, true)
                .map(Profile::getId);
    }
}
