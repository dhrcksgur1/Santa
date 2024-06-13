package com.example.santa.domain.user.service;

import com.example.santa.domain.category.entity.Category;
import com.example.santa.domain.category.repository.CategoryRepository;
import com.example.santa.domain.preferredcategory.dto.PreferredCategoryRequestDto;
import com.example.santa.domain.preferredcategory.dto.PreferredCategoryResponseDto;
import com.example.santa.domain.preferredcategory.entity.PreferredCategory;
import com.example.santa.domain.preferredcategory.repository.PreferredCategoryRepository;
import com.example.santa.domain.rank.dto.RankingResponseDto;
import com.example.santa.domain.rank.entity.Ranking;
import com.example.santa.domain.rank.repository.RankingRepository;
import com.example.santa.domain.report.entity.Report;
import com.example.santa.domain.report.repository.ReportRepository;
import com.example.santa.domain.user.dto.*;
import com.example.santa.domain.user.entity.Password;
import com.example.santa.domain.user.entity.Role;
import com.example.santa.domain.user.entity.User;
import com.example.santa.domain.user.repository.UserRepository;
import com.example.santa.domain.userchallenge.dto.UserChallengeCompletionResponseDto;
import com.example.santa.domain.usermountain.dto.UserMountainResponseDto;
import com.example.santa.global.constant.Constants;
import com.example.santa.global.exception.ExceptionCode;
import com.example.santa.global.exception.ServiceLogicException;
import com.example.santa.global.security.jwt.JwtToken;
import com.example.santa.global.security.jwt.JwtTokenProvider;
import com.example.santa.global.util.S3ImageService;
import com.example.santa.global.util.mapsturct.PreferredCategoryResponseDtoMapper;
import com.example.santa.global.util.mapsturct.UserChallengeCompletionResponseMapper;
import com.example.santa.global.util.mapsturct.UserMountainResponseDtoMapper;
import com.example.santa.global.util.mapsturct.UserResponseDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserResponseDtoMapper userResponseDtoMapper;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PreferredCategoryRepository preferredCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final RankingRepository rankingRepository;
    private final S3ImageService s3ImageService;
    private final ReportRepository reportRepository;
    private final UserMountainResponseDtoMapper userMountainResponseDtoMapper;
    private final PreferredCategoryResponseDtoMapper preferredCategoryResponseDtoMapper;
    private final UserChallengeCompletionResponseMapper userChallengeCompletionResponseMapper;

    @Transactional
    @Override
    public Long signup(UserSignupRequestDto request) {
        if (checkEmailDuplicate(request.getEmail())) {
            throw new ServiceLogicException(ExceptionCode.EMAIL_ALREADY_EXISTS);
        }
        if (checkNicknameDuplicate(request.getNickname())) {
            throw new ServiceLogicException(ExceptionCode.NICKNAME_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(new Password(request.getPassword()))
                .name(request.getName())
                .nickname(request.getNickname())
                .phoneNumber(request.getPhoneNumber())
                .image(Constants.DEFAULT_URL + "user_default_image.png")
                .role(Role.USER)
                .socialType(request.getSocialType())
                .socialId(request.getSocialId())
                .build();

        User save = userRepository.save(user);
        return save.getId();
    }

    @Override
    public Boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public Boolean checkNicknameDuplicate(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    /*
     * email + password 를 받아서 Authentication 객체 생성
     * authenticate 메서드 를 사용해서 User 검증 -> 이때 만들어 놓은 loadUserByUsername 메서드가 실행 됨
     * 검증 성공하면 Authentication 객체를 기반으로 JWT 토큰 생성
     * */
    @Transactional
    @Override
    public JwtToken signIn(UserSignInRequestDto userSignInRequestDto) {
        User user = userRepository.findByEmail(userSignInRequestDto.getEmail())
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.USER_NOT_FOUND));
        UsernamePasswordAuthenticationToken authenticationToken
                = new UsernamePasswordAuthenticationToken(userSignInRequestDto.getEmail(), userSignInRequestDto.getPassword(), user.getAuthorities());
        authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        JwtToken jwtToken = jwtTokenProvider.generateToken(authenticationToken);
        return jwtToken;
    }

    @Transactional
    @Override
    public String generateAccessToken(String refreshToken) {
        return jwtTokenProvider.generateAccessTokenFromRefreshToken(refreshToken);
    }

    @Override
    public UserResponseDto findUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.USER_NOT_FOUND));
        log.info("role {}", user.getRole());
        UserResponseDto dto = userResponseDtoMapper.toDto(user);
        log.info("dto = {}", dto);
        return dto;
    }

    @Override
    public Page<UserResponseDto> findAllUser(String search, Pageable pageable) {
        if (StringUtils.hasText(search)) {
            return userRepository.findAllByNameContainingOrNicknameContaining(search, search, pageable).map(userResponseDtoMapper::toDto);
        }
        return userRepository.findAll(pageable).map(userResponseDtoMapper::toDto);
    }

    @Override
    public Page<UserReportResponseDto> findAllReportUser(String search, Pageable pageable) {
        if (StringUtils.hasText(search)) {
            return userRepository.findUsersWithReportCount(search, pageable);
        } else {
            return userRepository.findUsersWithReportCount("", pageable);
        }
    }


    @Transactional
    @Override
    public UserResponseDto updateUser(String email, UserUpdateRequestDto userUpdateRequestDto) {
        MultipartFile imageFile = userUpdateRequestDto.getImageFile();
        String imageUrl = userUpdateRequestDto.getImage();

        if (imageFile != null && !imageFile.isEmpty()) {
            if (!Objects.equals(imageUrl, Constants.DEFAULT_URL + "user_default_image.png")) {
                s3ImageService.deleteImageFromS3(imageUrl);
            }
            imageUrl = s3ImageService.upload(imageFile);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.USER_NOT_FOUND))
                .update(userUpdateRequestDto.getNickname()
                        , userUpdateRequestDto.getPhoneNumber()
                        , userUpdateRequestDto.getName()
                        , imageUrl);

        return userResponseDtoMapper.toDto(user);
    }

    @Transactional
    @Override
    public void deleteUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.USER_NOT_FOUND));
        // 해당 사용자가 신고된 이력이 있는지 확인
        List<Report> reports = reportRepository.findByReportedParticipant(user);
        if (!reports.isEmpty()) {
            // 신고된 이력이 있다면 탈퇴 처리를 막음
            throw new ServiceLogicException(ExceptionCode.USER_REPORT_EXIST);
        }
        userRepository.delete(user);
    }

    @Transactional
    @Override
    public void deleteUserFromAdmin(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    @Override
    public String changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.USER_NOT_FOUND));
        user.getPasswordForChange().changePassword(oldPassword, newPassword);
        return user.getEmail();
    }

    @Transactional
    @Override
    public String resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.USER_NOT_FOUND));
        user.getPasswordForChange().resetPassword(newPassword);
        return user.getEmail();
    }

    @Transactional
    @Override
    public Long savePreferredCategory(String email, PreferredCategoryRequestDto preferredCategoryRequestDto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.USER_NOT_FOUND));
        Category category = categoryRepository.findById(preferredCategoryRequestDto.getCategoryId())
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.CATEGORY_NOT_FOUND));
        PreferredCategory save = preferredCategoryRepository.save(PreferredCategory.builder()
                .user(user)
                .category(category)
                .build());
        return save.getId();
    }

    @Transactional
    @Override
    public List<Long> savePreferredCategories(String email, List<Long> categoryIds) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.USER_NOT_FOUND));
        List<Long> createdPreferredCategories =
                categoryIds.stream().map((id) ->
                        preferredCategoryRepository.save(
                                PreferredCategory.builder()
                                        .user(user)
                                        .category(categoryRepository.findById(id).orElseThrow(() -> new ServiceLogicException(ExceptionCode.CATEGORY_NOT_FOUND)))
                                        .build()).getId()).toList();

        return createdPreferredCategories;
    }

    @Override
    public List<PreferredCategoryResponseDto> findAllPreferredCategories(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.USER_NOT_FOUND));
        List<PreferredCategory> allByUserId = preferredCategoryRepository.findAllByUserId(user.getId());
        List<PreferredCategoryResponseDto> dtoList = preferredCategoryResponseDtoMapper.toDtoList(allByUserId);
        return dtoList;
    }

    @Transactional
    @Override
    public void deleteAllPreferredCategory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.USER_NOT_FOUND));
        List<PreferredCategory> allByUserId = preferredCategoryRepository.findAllByUserId(user.getId());
        preferredCategoryRepository.deleteAll(allByUserId);
    }

    @Override
    public Page<UserMountainResponseDto> findAllUserMountains(String email, Pageable pageable) {
        User byEmail = userRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.USER_NOT_FOUND));
        Page<UserMountainResponseDto> pageDto = userRepository.findUserMountainsByUserId(byEmail.getId(), pageable)
                .map(userMountainResponseDtoMapper::toDto);
        return pageDto;
    }

    @Override
    public Page<UserChallengeCompletionResponseDto> findChallengesByCompletion(String email, boolean completion, Pageable pageable) {
        User byEmail = userRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.USER_NOT_FOUND));
        Page<UserChallengeCompletionResponseDto> completionDto;
        if (completion) {
            completionDto = userRepository.findByUserIdAndIsCompletedTrue(byEmail.getId(), pageable)
                    .map(userChallengeCompletionResponseMapper::toDto);
        } else {
            completionDto = userRepository.findByUserIdAndIsCompletedNull(byEmail.getId(), pageable)
                    .map(userChallengeCompletionResponseMapper::toDto);
        }
        return completionDto;
    }

    @Override
    public RankingResponseDto getIndividualRanking(String email) {
        List<Ranking> rankings = rankingRepository.findAllByOrderByScoreDesc();
        long rank = 1;
        for (Ranking ranking : rankings) {
            if (ranking.getUser().getEmail().equals(email)) {
                return new RankingResponseDto(rank, ranking.getId(), ranking.getUser().getNickname(), ranking.getUser().getImage(), ranking.getScore());
            }
            rank++;
        }
        throw new ServiceLogicException(ExceptionCode.USERRANKING_NOT_FOUND);
    }
}
