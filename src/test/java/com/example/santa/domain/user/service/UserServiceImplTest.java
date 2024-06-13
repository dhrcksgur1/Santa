package com.example.santa.domain.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.santa.domain.category.entity.Category;
import com.example.santa.domain.category.repository.CategoryRepository;
import com.example.santa.domain.challege.entity.Challenge;
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
import com.example.santa.domain.userchallenge.entity.UserChallenge;
import com.example.santa.domain.userchallenge.service.UserChallengeService;
import com.example.santa.domain.usermountain.dto.UserMountainResponseDto;
import com.example.santa.domain.usermountain.entity.UserMountain;
import com.example.santa.domain.usermountain.service.UserMountainService;
import com.example.santa.domain.usermountain.service.UserMountainServiceImpl;
import com.example.santa.global.exception.ServiceLogicException;
import com.example.santa.global.security.jwt.JwtToken;
import com.example.santa.global.security.jwt.JwtTokenProvider;
import com.example.santa.global.util.S3ImageService;
import com.example.santa.global.util.mapsturct.PreferredCategoryResponseDtoMapper;
import com.example.santa.global.util.mapsturct.UserChallengeCompletionResponseMapper;
import com.example.santa.global.util.mapsturct.UserMountainResponseDtoMapper;
import com.example.santa.global.util.mapsturct.UserResponseDtoMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RankingRepository rankingRepository;

    @Mock
    private UserResponseDtoMapper userResponseDtoMapper;
    @Mock
    private UserMountainResponseDtoMapper userMountainResponseDtoMapper;
    @Mock
    private UserChallengeCompletionResponseMapper userChallengeCompletionResponseMapper;
    @Mock
    private AuthenticationManagerBuilder authenticationManagerBuilder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private S3ImageService s3ImageService;
    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserMountainService userMountainService;

    @Mock
    private PreferredCategoryRepository preferredCategoryRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PreferredCategoryResponseDtoMapper preferredCategoryResponseDtoMapper;


    @InjectMocks
    private UserServiceImpl userService;
    private User user;

    private User rankingUser;
    private UserMountain userMountain;
    private Challenge challenge;
    private UserChallenge userChallenge;
    private Ranking ranking;
    private Authentication authentication;
    private UserSignupRequestDto requestDto;
    private UserUpdateRequestDto updateRequestDto;
    private UserSignInRequestDto signInRequestDto;
    private UserResponseDto userResponseDto;
    private UserMountainResponseDto userMountainresponseDto;
    private UserChallengeCompletionResponseDto userChallengeCompletionResponseDto;
    private String existingImageUrl;
    private String newImageUrl;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(1L);
        user.setEmail("user@email.com");
        user.setPassword(new Password("Password1!"));
        user.setName("test");
        user.setNickname("testNickName");
        user.setPhoneNumber("01011111111");
        user.setPassword(new Password("Password1!"));
        user.setRole(Role.ADMIN);
        user.setImage("http://example.com/original-image.jpg");
        user.setAccumulatedHeight(1000);

        requestDto = new UserSignupRequestDto();
        requestDto.setEmail("user@email.com");
        requestDto.setPassword("Password1!");
        requestDto.setName("test");
        requestDto.setNickname("testNickName");
        requestDto.setPhoneNumber("01011111111");

        signInRequestDto = new UserSignInRequestDto();
        signInRequestDto.setEmail("user@email.com");
        signInRequestDto.setPassword("Password1!");

        updateRequestDto = new UserUpdateRequestDto();
        updateRequestDto.setNickname("NewNickname");
        updateRequestDto.setPhoneNumber("01022222222");
        updateRequestDto.setName("NewName");
        updateRequestDto.setImageFile(null);
        updateRequestDto.setImage(existingImageUrl);

        userResponseDto = new UserResponseDto();  // 초기화 추가
        userResponseDto.setEmail(user.getEmail());
        userResponseDto.setNickname(user.getNickname());
        userResponseDto.setName(user.getName());
        userResponseDto.setPhoneNumber(user.getPhoneNumber());
        userResponseDto.setAccumulatedHeight(user.getAccumulatedHeight());

        userMountainresponseDto = new UserMountainResponseDto();
        userMountainresponseDto.setClimbDate(LocalDate.now());
        userMountainresponseDto.setMountainHeight(1000.1);
        userMountainresponseDto.setMountainName("관악산");
        userMountainresponseDto.setMountainLocation("서울시 관악구");
        userMountainresponseDto.setId(1L);


        userMountain = new UserMountain();
        Category category = new Category();
        category.setId(1L);
        category.setName("100대 명산 등산");

        userMountain.setCategory(category);

        challenge = new Challenge();
        challenge.setId(1L);
        challenge.setClearStandard(5);

        userChallenge = new UserChallenge();
        userChallenge.setProgress(0);
        userChallenge.setUser(user);
        userChallenge.setChallenge(challenge);

        ranking = new Ranking();
        ranking.setUser(user);
        ranking.setScore(1000);


        existingImageUrl = "http://example.com/original-image.jpg";
        newImageUrl = "http://example.com/new-uploaded-image.jpg";

//        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(user));
        lenient().when(authenticationManagerBuilder.getObject()).thenReturn(authenticationManager);

    }

    @Test
    void testSignup_Success() {
        when(userService.checkEmailDuplicate(anyString())).thenReturn(false);
        when(userService.checkNicknameDuplicate(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User user = i.getArgument(0);
            user.setId(1L);
            return user;
        });

        Long userId = userService.signup(requestDto);
        assertNotNull(userId);
        assertEquals(1L, userId);
    }

    @Test
    public void testSignup_EmailAlreadyExists() {
        // Given
        UserSignupRequestDto requestDto = new UserSignupRequestDto();
        requestDto.setEmail("existinguser@gmail.com"); // 이미 존재하는 이메일을 사용
        requestDto.setPassword("1q2w3e4r!!");
        requestDto.setName("엘리스");
        requestDto.setNickname("귀요미산악꾼");
        requestDto.setPhoneNumber("01023234545");

        when(userRepository.existsByEmail(anyString())).thenReturn(true); // 이미 존재하는 이메일을 가정

        // When & Then
        assertThrows(ServiceLogicException.class, () -> {
            userService.signup(requestDto);}
        );
    }

    @Test
    public void signup_NicknameAlreadyExists() {
        // Given
        UserSignupRequestDto requestDto = new UserSignupRequestDto();
        requestDto.setEmail("newuser@gmail.com");
        requestDto.setPassword("1q2w3e4r!!");
        requestDto.setName("엘리스");
        requestDto.setNickname("existingnickname"); // 이미 존재하는 닉네임을 사용
        requestDto.setPhoneNumber("01023234545");

        when(userRepository.existsByEmail(anyString())).thenReturn(false); // 이메일 중복 체크는 통과
        when(userRepository.existsByNickname(anyString())).thenReturn(true); // 이미 존재하는 닉네임을 가정

        // When & Then
        assertThrows(ServiceLogicException.class, () -> {
            userService.signup(requestDto);}
        );
    }


    @Test
    void testCheckEmailDuplicate_WhenExists_ReturnsTrue() {
        // Arrange
//        String email = "user@example.com";
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(true);
        // Act
        Boolean result = userService.checkEmailDuplicate(user.getEmail());

        // Assert
        assertTrue(result);
        verify(userRepository).existsByEmail(user.getEmail());
    }

    @Test
    void testCheckEmailDuplicate_WhenNotExists_ReturnsFalse() {
        // Arrange
        String email = "user@example.com";
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(false);

        // Act
        Boolean result = userService.checkEmailDuplicate(user.getEmail());

        // Assert
        assertFalse(result);
        verify(userRepository).existsByEmail(user.getEmail());
    }

    @Test
    void testCheckNicknameDuplicate_WhenExists_ReturnsTrue() {
        // Arrange
        String nickname = "nickname";
        when(userRepository.existsByNickname(user.getNickname())).thenReturn(true);

        // Act
        Boolean result = userService.checkNicknameDuplicate(user.getNickname());

        // Assert
        assertTrue(result);
        verify(userRepository).existsByNickname(user.getNickname());
    }

    @Test
    void testCheckNicknameDuplicate_WhenNotExists_ReturnsFalse() {
        // Arrange
        String nickname = "nickname";
        when(userRepository.existsByNickname(user.getNickname())).thenReturn(false);

        // Act
        Boolean result = userService.checkNicknameDuplicate(user.getNickname());

        // Assert
        assertFalse(result);
        verify(userRepository).existsByNickname(user.getNickname());
    }

    @Test
    void signIn_Success() {

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        // Arrange
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                signInRequestDto.getEmail(), signInRequestDto.getPassword(), user.getAuthorities());

        // Mock the Authentication object that is expected to be returned by the authenticationManager
        Authentication authenticated = Mockito.mock(Authentication.class);
        when(authenticationManager.authenticate(authenticationToken)).thenReturn(authenticated);

        JwtToken expectedToken = new JwtToken("Bearer", "dummy-access-token", "d", "ADMIN");

        // Use lenient to avoid strict stubbing errors, matching any Authentication object
        Mockito.lenient().when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn(expectedToken);

        // Act
        JwtToken actualToken = userService.signIn(signInRequestDto);

        // Assert
        assertNotNull(actualToken);
        assertEquals(expectedToken, actualToken);

        verify(userRepository).findByEmail(signInRequestDto.getEmail());
        verify(authenticationManager).authenticate(authenticationToken);

        // Adjust verification to match any Authentication object rather than the specific mock
        verify(jwtTokenProvider).generateToken(any(Authentication.class));  // This change ensures that any Authentication object is accepted
    }
    @Test
    void signIn_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.empty());
        // Act & Assert
        Exception exception = assertThrows(ServiceLogicException.class, () -> userService.signIn(signInRequestDto));
        assertEquals("존재하지 않는 회원입니다.", exception.getMessage());
    }

    @Test
    void findUserByEmail_UserExists_ReturnsDto() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userResponseDtoMapper.toDto(any(User.class))).thenReturn(userResponseDto);
        // Act
        UserResponseDto result = userService.findUserByEmail(user.getEmail());

        // Assert
        assertNotNull(result);
        assertEquals(userResponseDto.getEmail(), result.getEmail());
        assertEquals(userResponseDto.getNickname(), result.getNickname());
        assertEquals(userResponseDto.getName(), result.getName());
        assertEquals(userResponseDto.getPhoneNumber(), result.getPhoneNumber());
        assertEquals(userResponseDto.getAccumulatedHeight(), result.getAccumulatedHeight());

        verify(userRepository).findByEmail(user.getEmail());
        verify(userResponseDtoMapper).toDto(user);
    }

    @Test
    void testFindAllReportUser_WithSearch_FilteredUsers() {
        // given
        String search = "test";
        Pageable pageable = PageRequest.of(0, 10);
        List<UserReportResponseDto> users = Arrays.asList(
                new UserReportResponseDto(1L, "test1@example.com", "nickname1", "name1", 2L),
                new UserReportResponseDto(2L, "test2@example.com", "nickname2", "name2", 3L)
        );
        Page<UserReportResponseDto> expectedPage = new PageImpl<>(users, pageable, users.size());
        when(userRepository.findUsersWithReportCount(search, pageable)).thenReturn(expectedPage);

        // when
        Page<UserReportResponseDto> result = userService.findAllReportUser(search, pageable);

        // then
        assertEquals(expectedPage, result);
        verify(userRepository, times(1)).findUsersWithReportCount(search, pageable);
    }

    @Test
    public void testFindAllReportUser_WithoutSearch_AllUsers() {
        // given
        String search = "";
        Pageable pageable = PageRequest.of(0, 10);
        List<UserReportResponseDto> users = Arrays.asList(
                new UserReportResponseDto(1L, "user1@example.com", "nickname1", "name1", 1L),
                new UserReportResponseDto(2L, "user2@example.com", "nickname2", "name2", 4L)
        );
        Page<UserReportResponseDto> expectedPage = new PageImpl<>(users, pageable, users.size());
        when(userRepository.findUsersWithReportCount(search, pageable)).thenReturn(expectedPage);

        // when
        Page<UserReportResponseDto> result = userService.findAllReportUser(search, pageable);

        // then
        assertEquals(expectedPage, result);
        verify(userRepository, times(1)).findUsersWithReportCount(search, pageable);
    }

    @Test
    void testChangePassword_Success() {
        // Given
        String email = user.getEmail();
        String oldPassword = "Password1!";
        String newPassword = "newPassword";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        // When
        String resultEmail = userService.changePassword(email, oldPassword, newPassword);
        // Then
        assertEquals(email, resultEmail);
    }

    @Test
    void testChangePassword_UserNotFound() {
        // Given
        String email = "nonexistent@email.com";
        String oldPassword = "Password1!";
        String newPassword = "newPassword";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        // When & Then
        assertThrows(
                ServiceLogicException.class, () -> {
                    userService.changePassword(email, oldPassword, newPassword);
                }
        );
    }

    @Test
    void testRestPassword_Success() {
        // Given
        String email = user.getEmail();
        String newPassword = "newPassword";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        // When
        String resultEmail = userService.resetPassword(email, newPassword);

        // Then
        assertEquals(email, resultEmail);
    }

    @Test
    void testRestPassword_UserNotFound() {
        // Given
        String email = "nonexistent@email.com";
        String newPassword = "newPassword";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        // When & Then
        assertThrows(
                ServiceLogicException.class, () -> {
                    userService.resetPassword(email, newPassword);
                }
        );
    }

    @Test
    void testDeleteUser_Success() {
        // Given
        String email = user.getEmail();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(reportRepository.findByReportedParticipant(user)).thenReturn(Collections.emptyList());

        // When
        userService.deleteUser(email);

        // Then
        verify(userRepository, times(1)).findByEmail(email);
        verify(reportRepository, times(1)).findByReportedParticipant(user);
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    void testDeletedUser_UserNotFound() {
        // Given
        String email = user.getEmail();
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ServiceLogicException.class, () -> userService.deleteUser(email));
        verify(userRepository, times(1)).findByEmail(email);
    }

    // 이메일로 사용자를 찾았지만 사용자가 신고된 이력이 있을때 예외발생
    @Test
    void testDeleted_UserHasReports() {
        // Given
        String email = user.getEmail();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(reportRepository.findByReportedParticipant(user)).thenReturn(List.of(new Report()));

        // When & Then
        assertThrows(ServiceLogicException.class, () -> userService.deleteUser(email));
        verify(userRepository, times(1)).findByEmail(email);
        verify(reportRepository, times(1)).findByReportedParticipant(user);
    }

    @Test
    void testDeleteUserFromAdmin_Success() {
        // Given
        Long userId = user.getId();
        // When
        userService.deleteUserFromAdmin(userId);
        // Then
        verify(userRepository, times(1)).deleteById(userId);
    }

    // 이미지, 사용자 정보 둘다 업데이트된 경우
    @Test
    public void testUpdateUser_WithImage_UpdatesUserAndImage() {
        // Given
        String email = user.getEmail();
        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes());
        UserUpdateRequestDto requestDto = new UserUpdateRequestDto();
        requestDto.setImageFile(imageFile);
        requestDto.setNickname("updatedNickname");
        requestDto.setPhoneNumber("01012345678");
        requestDto.setName("updatedName");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(s3ImageService.upload(any(MultipartFile.class))).thenReturn("newImageUrl");
        when(userResponseDtoMapper.toDto(any(User.class))).thenReturn(userResponseDto);

        // When
        UserResponseDto result = userService.updateUser(email, requestDto);

        // Then
        assertNotNull(result);
        verify(s3ImageService, times(1)).upload(any(MultipartFile.class));
        verify(userRepository, times(1)).findByEmail(email);
    }

    // 이미지 x, 사용자 정보만 업데이트된 경우
    @Test
    public void testUpdateUser_WithoutImage_UpdatesUserInfo() {
        // given
        String email = user.getEmail();
        UserUpdateRequestDto requestDto = new UserUpdateRequestDto();
        requestDto.setNickname("updateNickname");
        requestDto.setPhoneNumber("01012345678");
        requestDto.setName("updateName");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userResponseDtoMapper.toDto(any(User.class))).thenReturn(userResponseDto);

        // When
        UserResponseDto result = userService.updateUser(email, requestDto);

        // Then
        assertNotNull(result);
        verify(s3ImageService, never()).upload(any(MultipartFile.class));
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    public void testUpdateUser_UserNotFound() {
        // Given
        String email = "nonexistent@example.com";
        UserUpdateRequestDto requestDto = new UserUpdateRequestDto();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ServiceLogicException.class, () -> userService.updateUser(email, requestDto));
    }

//    @Test
//    void testFindAllUserMountains() {
//        // Given
//        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").descending());
//        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
//        when(userRepository.findUserMountainsByUserId(any(Long.class), any(Pageable.class)))
//                .thenReturn(new PageImpl<>(Arrays.asList(userMountain)));
//        when(userMountainResponseDtoMapper.toDto(any(UserMountain.class))).thenReturn(userMountainresponseDto);
//
//        // When
//        Page<UserMountainResponseDto> result = userService.findAllUserMountains(user.getEmail(), pageable);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(1, result.getTotalElements());
//        assertEquals(userMountainresponseDto, result.getContent().get(0));
//
//        verify(userRepository, times(1)).findByEmail(user.getEmail());
//        verify(userRepository, times(1)).findUserMountainsByUserId(user.getId(), pageable);
//        verify(userMountainResponseDtoMapper, times(1)).toDto(userMountain);
//    }

    @Test
    void testFindAllUserMountains() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").descending());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.findUserMountainsByUserId(any(Long.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(userMountain)));
        when(userMountainResponseDtoMapper.toDto(eq(userMountain))).thenReturn(userMountainresponseDto);


        // When
        Page<UserMountainResponseDto> result = userService.findAllUserMountains(user.getEmail(), pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(userMountainresponseDto, result.getContent().get(0));

        verify(userRepository, times(1)).findByEmail(user.getEmail());
        verify(userRepository, times(1)).findUserMountainsByUserId(user.getId(), pageable);
        verify(userMountainResponseDtoMapper, times(1)).toDto(userMountain);
    }

    @Test
    void testFindChallengesByCompletion() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").descending());

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.findByUserIdAndIsCompletedTrue(user.getId(), pageable))
                .thenReturn(new PageImpl<>(Arrays.asList(userChallenge)));
        when(userRepository.findByUserIdAndIsCompletedNull(user.getId(), pageable))
                .thenReturn(new PageImpl<>(Arrays.asList(userChallenge)));
        when(userChallengeCompletionResponseMapper.toDto(userChallenge)).thenReturn(userChallengeCompletionResponseDto);

        Page<UserChallengeCompletionResponseDto> resultCompleted = userService.findChallengesByCompletion(user.getEmail(), true, pageable);
        assertNotNull(resultCompleted);
        assertEquals(1, resultCompleted.getTotalElements());
        assertEquals(userChallengeCompletionResponseDto, resultCompleted.getContent().get(0));

        Page<UserChallengeCompletionResponseDto> resultIncomplete = userService.findChallengesByCompletion(user.getEmail(), false, pageable);
        assertNotNull(resultIncomplete);
        assertEquals(1, resultIncomplete.getTotalElements());
        assertEquals(userChallengeCompletionResponseDto, resultIncomplete.getContent().get(0));

        verify(userRepository, times(2)).findByEmail(user.getEmail());
        verify(userRepository, times(1)).findByUserIdAndIsCompletedTrue(user.getId(), pageable);
        verify(userRepository, times(1)).findByUserIdAndIsCompletedNull(user.getId(), pageable);
        verify(userChallengeCompletionResponseMapper, times(2)).toDto(userChallenge);  // Adjust to expect 2 invocations

    }

    @Test
    void testGetIndividualRanking() {
        // Mock data

        rankingUser = new User();
        rankingUser.setId(2L);
        rankingUser.setEmail("ranking@email.com");
        rankingUser.setName("ranking");
        rankingUser.setNickname("ranking");
        rankingUser.setPhoneNumber("01011111111");
        rankingUser.setPassword(new Password("Password1!"));
        rankingUser.setRole(Role.ADMIN);
        rankingUser.setImage("http://example.com/original-image.jpg");
        rankingUser.setAccumulatedHeight(1000);

        Ranking ranking1 = new Ranking();
        ranking1.setId(1L);
        ranking1.setScore(1000);
        ranking1.setUser(user);

        Ranking ranking2 = new Ranking();
        ranking2.setId(2L);
        ranking2.setScore(1500);
        ranking2.setUser(rankingUser);

        List<Ranking> rankings = new ArrayList<>();
        rankings.add(ranking1);
        rankings.add(ranking2);

        // Mock behavior
        when(rankingRepository.findAllByOrderByScoreDesc()).thenReturn(rankings);

        // Test
        RankingResponseDto expectedResponseDto = new RankingResponseDto(1L, ranking1.getId(), ranking1.getUser().getNickname(), ranking1.getUser().getImage(), ranking1.getScore());
        RankingResponseDto actualResponseDto = userService.getIndividualRanking(user.getEmail());

//        assertEquals(expectedResponseDto, actualResponseDto);
        assertEquals(expectedResponseDto.getId(), actualResponseDto.getId());
        assertEquals(expectedResponseDto.getNickname(), actualResponseDto.getNickname());
        assertEquals(expectedResponseDto.getImage(), actualResponseDto.getImage());
        assertEquals(expectedResponseDto.getScore(), actualResponseDto.getScore());
        verify(rankingRepository, times(1)).findAllByOrderByScoreDesc();
    }

    @Test
    void testSavePreferredCategories() {
        // Given
        List<Long> categoryIds = Arrays.asList(1L, 2L, 3L);
        Category category1 = Category.builder().id(1L).build();
        Category category2 = Category.builder().id(2L).build();
        Category category3 = Category.builder().id(3L).build();
        List<Long> createdPreferredCategories = Arrays.asList(11L, 12L, 13L);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category1));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category2));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category3));
        when(preferredCategoryRepository.save(any(PreferredCategory.class))).thenReturn(PreferredCategory.builder().id(11L).build())
                .thenReturn(PreferredCategory.builder().id(12L).build())
                .thenReturn(PreferredCategory.builder().id(13L).build());

        // When
        List<Long> result = userService.savePreferredCategories(user.getEmail(), categoryIds);

        // Then
        assertEquals(result, createdPreferredCategories);
        verify(userRepository, times(1)).findByEmail(user.getEmail());
        verify(categoryRepository, times(3)).findById(anyLong());
        verify(preferredCategoryRepository, times(3)).save(any(PreferredCategory.class));
    }
    @Test
    void findAllPreferredCategories() {
        // Given
        PreferredCategory preferredCategory1 = PreferredCategory.builder().id(1L).user(user).category(Category.builder().id(1L).name("등산").build()).build();
        PreferredCategory preferredCategory2 = PreferredCategory.builder().id(2L).user(user).category(Category.builder().id(2L).name("식도릭").build()).build();
        List<PreferredCategory> preferredCategories = Arrays.asList(preferredCategory1, preferredCategory2);
        List<PreferredCategoryResponseDto> expectedDtoList = new ArrayList<>();
        PreferredCategoryResponseDto dto1 = new PreferredCategoryResponseDto();
        dto1.setCategoryName("등산");
        PreferredCategoryResponseDto dto2 = new PreferredCategoryResponseDto();
        dto2.setCategoryName("식도락");
        expectedDtoList.add(dto1);
        expectedDtoList.add(dto2);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(preferredCategoryRepository.findAllByUserId(user.getId())).thenReturn(preferredCategories);
        when(preferredCategoryResponseDtoMapper.toDtoList(preferredCategories)).thenReturn(expectedDtoList);

        // When
        List<PreferredCategoryResponseDto> result = userService.findAllPreferredCategories(user.getEmail());

        // Then
        assertEquals(result, expectedDtoList);
        verify(userRepository, times(1)).findByEmail(user.getEmail());
        verify(preferredCategoryRepository, times(1)).findAllByUserId(user.getId());
        verify(preferredCategoryResponseDtoMapper, times(1)).toDtoList(preferredCategories);
    }

    @Test
    void deleteAllPreferredCategory() {
        // Given
        PreferredCategory preferredCategory1 = PreferredCategory.builder().id(1L).user(user).build();
        PreferredCategory preferredCategory2 = PreferredCategory.builder().id(2L).user(user).build();
        List<PreferredCategory> preferredCategories = Arrays.asList(preferredCategory1, preferredCategory2);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(preferredCategoryRepository.findAllByUserId(user.getId())).thenReturn(preferredCategories);

        // When
        userService.deleteAllPreferredCategory(user.getEmail());

        // Then
        verify(userRepository, times(1)).findByEmail(user.getEmail());
        verify(preferredCategoryRepository, times(1)).findAllByUserId(user.getId());
        verify(preferredCategoryRepository, times(1)).deleteAll(preferredCategories);
    }
}

