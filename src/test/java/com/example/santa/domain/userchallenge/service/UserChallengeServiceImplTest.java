package com.example.santa.domain.userchallenge.service;

import com.example.santa.domain.category.entity.Category;
import com.example.santa.domain.challege.entity.Challenge;
import com.example.santa.domain.challege.repository.ChallengeRepository;
import com.example.santa.domain.meeting.entity.Meeting;
import com.example.santa.domain.meeting.repository.MeetingRepository;
import com.example.santa.domain.user.entity.User;
import com.example.santa.domain.user.repository.UserRepository;
import com.example.santa.domain.userchallenge.entity.UserChallenge;
import com.example.santa.domain.userchallenge.repository.UserChallengeRepository;
import com.example.santa.domain.usermountain.entity.UserMountain;
import com.example.santa.domain.usermountain.repository.UserMountainRepository;
import com.example.santa.global.exception.ExceptionCode;
import com.example.santa.global.exception.ServiceLogicException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.*;


@ExtendWith(MockitoExtension.class)
class UserChallengeServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMountainRepository userMountainRepository;
    @Mock
    private ChallengeRepository challengeRepository;
    @Mock
    private UserChallengeRepository userChallengeRepository;

    @Mock
    private MeetingRepository meetingRepository;


    @InjectMocks
    private UserChallengeServiceImpl userChallengeService;

    private User user;
    private UserMountain userMountain;
    private Challenge challenge;

    private Meeting meeting;
    private UserChallenge userChallenge;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");

        userMountain = new UserMountain();

        Category category = new Category();
        category.setId(1L);
        category.setName("100대 명산 등산");

        userMountain.setCategory(category);

        challenge = new Challenge();
        challenge.setId(1L);
        challenge.setClearStandard(2);

        userChallenge = new UserChallenge();
        userChallenge.setProgress(0);
        userChallenge.setUser(user);
        userChallenge.setChallenge(challenge);
        userChallenge.setIsCompleted(false);

        meeting = new Meeting();
        meeting.setId(1L);
        meeting.setCategory(category);

    }


    // 새로운 UserChallenge 생성 및 진행률 증가: 새로운 UserChallenge가 생성되고 진행률이 증가하는지 확인합니다.
    @Test
    void updateProgress_NewUserChallengeCreatedAndProgressIncremented_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userMountainRepository.findById(anyLong())).thenReturn(Optional.of(userMountain));
        when(challengeRepository.findByCategoryName(anyString())).thenReturn(Arrays.asList(challenge));
        when(userChallengeRepository.findByUserAndChallengeId(any(User.class), anyLong())).thenReturn(Optional.empty());
        when(userChallengeRepository.save(any(UserChallenge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userChallengeService.updateProgress("test@example.com", 1L);

        ArgumentCaptor<UserChallenge> captor = ArgumentCaptor.forClass(UserChallenge.class);
        verify(userChallengeRepository, times(2)).save(captor.capture());

        List<UserChallenge> savedChallenges = captor.getAllValues();

        UserChallenge createdChallenge = savedChallenges.get(0);
        assertEquals(1, createdChallenge.getProgress());
        assertFalse(Boolean.TRUE.equals(createdChallenge.getIsCompleted()));
    }

    // 진행률 증가 및 챌린지 완료: 진행률이 증가하고 명확한 기준에 도달하면 챌린지가 완료로 표시되는지 확인합니다.
    @Test
    void updateProgress_ProgressIncrementedAndChallengeCompleted_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userMountainRepository.findById(anyLong())).thenReturn(Optional.of(userMountain));
        when(challengeRepository.findByCategoryName(anyString())).thenReturn(Arrays.asList(challenge));
        when(userChallengeRepository.findByUserAndChallengeId(any(User.class), anyLong())).thenReturn(Optional.of(userChallenge));
        when(userChallengeRepository.save(any(UserChallenge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userChallenge.setProgress(challenge.getClearStandard() - 1);

        userChallengeService.updateProgress("test@example.com", 1L);

        ArgumentCaptor<UserChallenge> captor = ArgumentCaptor.forClass(UserChallenge.class);
        verify(userChallengeRepository, times(1)).save(captor.capture());

        UserChallenge updatedChallenge = captor.getValue();
        assertEquals(challenge.getClearStandard(), updatedChallenge.getProgress());
        assertTrue(Boolean.TRUE.equals(updatedChallenge.getIsCompleted()));
        assertEquals(LocalDate.now(), updatedChallenge.getCompletionDate());
    }

    @Test
    void updateProgress_ThrowsExceptionIfUserNotFound() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        Exception exception = assertThrows(ServiceLogicException.class, () -> {
            userChallengeService.updateProgress("nonexistent@example.com", 1L);
        });

        assertEquals("존재하지 않는 회원입니다.", exception.getMessage());

    }

    @Test
    void updateProgress_ThrowsExceptionIfUserMountainNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userMountainRepository.findById(1L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(ServiceLogicException.class, () -> {
            userChallengeService.updateProgress("test@example.com", 1L);
        });
        assertEquals("존재하지 않는 유저 등산 정보입니다.", exception.getMessage());
        verify(userChallengeRepository, never()).save(any(UserChallenge.class));
    }


    //새로운 UserChallenge 생성 및 진행률 증가: 새로운 UserChallenge가 생성되고 진행률이 증가하는지 확인합니다.
    @Test
    void updateUserChallengeOnMeetingJoin_NewUserChallengeCreatedAndProgressIncremented() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(meetingRepository.findById(anyLong())).thenReturn(Optional.of(meeting));
        when(challengeRepository.findByCategoryName(anyString())).thenReturn(Arrays.asList(challenge));
        when(userChallengeRepository.findByUserAndChallengeId(any(User.class), anyLong())).thenReturn(Optional.empty());
        when(userChallengeRepository.save(any(UserChallenge.class))).thenAnswer(invocation -> invocation.getArgument(0));


        userChallengeService.updateUserChallengeOnMeetingJoin(1L, 1L);

        ArgumentCaptor<UserChallenge> captor = ArgumentCaptor.forClass(UserChallenge.class);
        verify(userChallengeRepository, times(2)).save(captor.capture());

        List<UserChallenge> savedChallenges = captor.getAllValues();

        UserChallenge createdChallenge = savedChallenges.get(0);
        assertEquals(1, createdChallenge.getProgress());
        assertNull(createdChallenge.getIsCompleted());

        UserChallenge updatedChallenge = savedChallenges.get(1);
        assertEquals(1, updatedChallenge.getProgress());
        assertNull(updatedChallenge.getIsCompleted());
    }

    //진행률 증가 및 챌린지 완료: 진행률이 증가하고 명확한 기준에 도달하면 챌린지가 완료로 표시되는지 확인합니다.
    @Test
    void updateUserChallengeOnMeetingJoin_ProgressIncrementedAndChallengeCompleted() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(meetingRepository.findById(anyLong())).thenReturn(Optional.of(meeting));
        when(userChallengeRepository.findByUserAndChallengeId(any(User.class), anyLong())).thenReturn(Optional.of(userChallenge));
        when(challengeRepository.findByCategoryName(anyString())).thenReturn(Arrays.asList(challenge));

        userChallenge.setProgress(challenge.getClearStandard() - 1);

        userChallengeService.updateUserChallengeOnMeetingJoin(1L, 1L);

        ArgumentCaptor<UserChallenge> captor = ArgumentCaptor.forClass(UserChallenge.class);
        verify(userChallengeRepository, times(1)).save(captor.capture());

        UserChallenge updatedChallenge = captor.getValue();
        assertEquals(challenge.getClearStandard(), updatedChallenge.getProgress());
        assertTrue(updatedChallenge.getIsCompleted());
        assertEquals(LocalDate.now(), updatedChallenge.getCompletionDate());
    }

    @Test
    void updateUserChallengeOnMeetingJoin_ThrowsExceptionIfUserNotFound() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(ServiceLogicException.class, () -> {
            userChallengeService.updateUserChallengeOnMeetingJoin(1L, 1L);
        });

        assertEquals("존재하지 않는 회원입니다." ,exception.getMessage());
    }

    @Test
    void updateUserChallengeOnMeetingJoin_ThrowsExceptionIfMeetingNotFound_() {
        when(meetingRepository.findById(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(ServiceLogicException.class, () -> {
            userChallengeService.updateUserChallengeOnMeetingJoin(1L, 1L);
        });

        assertEquals("모임을 찾을 수 없습니다.", exception.getMessage());
    }

}


