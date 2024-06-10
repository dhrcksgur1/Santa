package com.example.santa.domain.meeting.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.*;

import com.example.santa.domain.category.entity.Category;
import com.example.santa.domain.category.repository.CategoryRepository;
import com.example.santa.domain.meeting.dto.MeetingDto;
import com.example.santa.domain.meeting.dto.MeetingResponseDto;
import com.example.santa.domain.meeting.dto.ParticipantDto;
import com.example.santa.domain.meeting.entity.Meeting;
import com.example.santa.domain.meeting.entity.MeetingTag;
import com.example.santa.domain.meeting.entity.Participant;
import com.example.santa.domain.meeting.entity.Tag;
import com.example.santa.domain.meeting.repository.MeetingRepository;
import com.example.santa.domain.meeting.repository.MeetingTagRepository;
import com.example.santa.domain.meeting.repository.ParticipantRepository;
import com.example.santa.domain.meeting.repository.TagRepository;
import com.example.santa.domain.user.entity.Role;
import com.example.santa.domain.user.entity.User;
import com.example.santa.domain.user.repository.UserRepository;
import com.example.santa.domain.userchallenge.service.UserChallengeService;
import com.example.santa.global.exception.ExceptionCode;
import com.example.santa.global.exception.ServiceLogicException;
import com.example.santa.global.util.S3ImageService;
import com.example.santa.global.util.mapsturct.ParticipantsDtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class MeetingServiceImplTest {

    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private MeetingTagRepository meetingTagRepository;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private ParticipantsDtoMapper participantsDtoMapper;
    @Mock
    private S3ImageService s3ImageService;
    @Mock
    private UserChallengeService userChallengeService;

    @InjectMocks
    private MeetingServiceImpl meetingService;

    private User leader;
    private User user;
    private User admin;
    private Category category;
    private Tag tag;
    private MeetingDto meetingDto;
    private Meeting meeting;

    private List<ParticipantDto> participantDtos;

    private List<Meeting> meetings;

    private Pageable pageable;

    @BeforeEach
    void setUp() {

        leader = new User();
        leader.setId(1L);
        leader.setEmail("test@email.com");
        leader.setRole(Role.USER);

        user = new User();
        user.setId(2L);
        user.setEmail("user@email.com");
        user.setRole(Role.USER);

        admin = new User();
        admin.setId(3L);
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);

        category = new Category();
        category.setId(1L);
        category.setName("등산");

        tag = new Tag();
        tag.setName("산행");

        String fileName = "test-image.jpg";
        String contentType = "image/jpeg";
        byte[] content = {0x01, 0x02, 0x03};
        MultipartFile imageFile = new MockMultipartFile(fileName, fileName, contentType, content);

        meetingDto = new MeetingDto();
        meetingDto.setMeetingName("산악회");
        meetingDto.setCategoryName("등산");
        meetingDto.setMountainName("북한산");
        meetingDto.setDescription("북한산 등산 후 식사");
        meetingDto.setHeadcount(15);
        meetingDto.setDate(LocalDate.of(2024, 5, 20));
        meetingDto.setTags(Collections.singletonList("산행"));
        meetingDto.setImageFile(imageFile);

        MeetingTag meetingTag = new MeetingTag();
        meetingTag.setTag(tag);


        Participant participant = new Participant();
        participant.setUser(leader);

        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setUserId(participant.getUser().getId());

        participantDtos = new ArrayList<>();
        participantDtos.add(participantDto);

        meeting = Meeting.builder()
                .id(1L)
                .meetingName("산악회")
                .leader(leader)
                .category(category)
                .mountainName("북한산")
                .description("북한산 등산 후 식사")
                .headcount(15)
                .date(LocalDate.of(2024, 5, 20))
                .meetingTags(new HashSet<>(Collections.singletonList(meetingTag))) // 초기화 및 값 할당
                .participant(new ArrayList<>(Collections.singletonList(participant))) // 초기화 및 값 할당
                .image("https://s3.amazonaws.com/test-bucket/test-image.jpg")
                .build();


        pageable = PageRequest.of(0, 10);
    }

    @Test
    void createMeeting_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(leader));
        when(categoryRepository.findByName("등산")).thenReturn(Optional.of(category));
        when(s3ImageService.upload(any(MultipartFile.class))).thenReturn("https://s3.amazonaws.com/test-bucket/test-image.jpg");
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
        when(tagRepository.findByName("산행")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(meetingTagRepository.save(any(MeetingTag.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MeetingResponseDto response = meetingService.createMeeting("test@example.com", meetingDto);
        response.setLeaderId(leader.getId());

        assertNotNull(response);
        assertEquals("산악회", response.getMeetingName());
        assertEquals("등산", response.getCategoryName());
        assertEquals("북한산", response.getMountainName());
        assertEquals("북한산 등산 후 식사", response.getDescription());
        assertEquals(15, response.getHeadcount());
        assertEquals(LocalDate.of(2024, 5, 20), response.getDate());
        assertEquals(1L, response.getLeaderId());
        assertEquals(1, response.getTags().size());
        assertEquals("산행", response.getTags().get(0));
        assertEquals("https://s3.amazonaws.com/test-bucket/test-image.jpg",response.getImage());
    }

    @Test
    void createMeeting_ThrowsExceptionIfParticipatingOnSameDate() {
        // 사용자가 이미 같은 날짜에 다른 모임에 참여중인 경우
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(leader));
        when(categoryRepository.findByName("등산")).thenReturn(Optional.of(category));
        when(userRepository.findMeetingsByUserId(leader.getId())).thenReturn(List.of(meeting));

        // 동일한 날짜에 다른 모임에 참여중이므로 예외가 발생해야 함
        assertThrows(ServiceLogicException.class, () -> {
            meetingService.createMeeting("test@example.com", meetingDto);
        });
    }


    @Test
    void updateMeeting_success() {
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
        when(categoryRepository.findByName("등산")).thenReturn(Optional.of(category));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(leader));

        Tag updatedTag = new Tag();
        updatedTag.setName("업데이트 된 산행");
        when(tagRepository.findByName("업데이트 된 산행")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(meetingTagRepository.save(any(MeetingTag.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MeetingDto updateDto = new MeetingDto();
        updateDto.setMeetingName("업데이트 된 산악회");
        updateDto.setCategoryName("등산");
        updateDto.setMountainName("설악산");
        updateDto.setDescription("설악산 등산 후 식사");
        updateDto.setHeadcount(20);
        updateDto.setDate(LocalDate.of(2024, 6, 15));
        updateDto.setTags(Collections.singletonList("업데이트 된 산행"));

        MeetingResponseDto response = meetingService.updateMeeting("test@example.com", 1L, updateDto);
        response.setLeaderId(leader.getId());


        assertNotNull(response);
        assertEquals("업데이트 된 산악회", response.getMeetingName());
        assertEquals("등산", response.getCategoryName());
        assertEquals("설악산", response.getMountainName());
        assertEquals("설악산 등산 후 식사", response.getDescription());
        assertEquals(20, response.getHeadcount());
        assertEquals(LocalDate.of(2024, 6, 15), response.getDate());
        assertEquals(1L, response.getLeaderId());
        assertEquals(1, response.getTags().size());
        assertEquals("업데이트 된 산행", response.getTags().get(0));
    }

    @Test
    void updateMeeting_ThrowsExceptionIfParticipatingOnSameDate() {
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(categoryRepository.findByName("등산")).thenReturn(Optional.of(category));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(leader));
        when(userRepository.findMeetingsByUserId(leader.getId())).thenReturn(List.of(meeting));


        MeetingDto updateDto = new MeetingDto();
        updateDto.setMeetingName("업데이트 된 산악회");
        updateDto.setCategoryName("등산");
        updateDto.setMountainName("설악산");
        updateDto.setDescription("설악산 등산 후 식사");
        updateDto.setHeadcount(20);
        updateDto.setDate(LocalDate.of(2024, 5, 20));


        assertThrows(ServiceLogicException.class, () -> {
            meetingService.updateMeeting("test@example.com", 1L, updateDto);
        });
    }

    //READ
    @Test
    void getAllMeetings_success() {
        List<Meeting> meetingList = Collections.singletonList(meeting);
        Page<Meeting> meetingPage = new PageImpl<>(meetingList, pageable, meetingList.size());

        when(meetingRepository.findAll(pageable)).thenReturn(meetingPage);

        Page<MeetingResponseDto> result = meetingService.getAllMeetings(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals("산악회", result.getContent().get(0).getMeetingName());
        assertEquals("북한산", result.getContent().get(0).getMountainName());
    }

    @Test
    void meetingDetail_success(){
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));

        MeetingResponseDto response = meetingService.meetingDetail(meeting.getId());
        response.setLeaderId(leader.getId());

        assertNotNull(response);
        assertEquals("산악회", response.getMeetingName());
        assertEquals("등산", response.getCategoryName());
        assertEquals("북한산", response.getMountainName());
        assertEquals("북한산 등산 후 식사", response.getDescription());
        assertEquals(15, response.getHeadcount());
        assertEquals(LocalDate.of(2024, 5, 20), response.getDate());
        assertEquals(1L, response.getLeaderId());

    }

    @Test
    void getMyMeetings_success() {
        Page<Meeting> meetingPage = new PageImpl<>(Collections.singletonList(meeting), pageable, 1);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(meetingRepository.findMeetingsByParticipantUserId(user.getId(), pageable)).thenReturn(meetingPage);

        Page<MeetingResponseDto> result = meetingService.getMyMeetings("test@example.com", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());  // 총 요소 개수 확인
        assertEquals("산악회", result.getContent().get(0).getMeetingName());  // 첫 번째 요소의 meetingName 확인

        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(meetingRepository, times(1)).findMeetingsByParticipantUserId(user.getId(), pageable);
    }

    @Test
    void getMeetingsByTagName_success() {
        List<Meeting> meetingList = Collections.singletonList(meeting);
        Page<Meeting> meetingPage = new PageImpl<>(meetingList, pageable, meetingList.size());

        when(meetingRepository.findByMeetingTags_Tag_NameContaining(tag.getName(), pageable)).thenReturn(meetingPage);

        Page<MeetingResponseDto> result = meetingService.getMeetingsByTagName(tag.getName(), pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("산악회", result.getContent().get(0).getMeetingName());

        verify(meetingRepository, times(1)).findByMeetingTags_Tag_NameContaining(tag.getName(), pageable);
    }

    @Test
    void getMeetingsByCategoryName_withCustomRecommendation() {
        String categoryName = "맞춤추천";

        Page<Meeting> meetingPage = new PageImpl<>(Collections.singletonList(meeting), pageable, 1);

        when(meetingRepository.findByUserEmailAndPreferredCategories(user.getEmail(), pageable)).thenReturn(meetingPage);

        Page<MeetingResponseDto> result = meetingService.getMeetingsByCategoryName(user.getEmail(), categoryName, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("산악회", result.getContent().get(0).getMeetingName());

        verify(meetingRepository, times(1)).findByUserEmailAndPreferredCategories(user.getEmail(), pageable);
    }

    @Test
    void getMeetingsByCategoryName_withSpecificCategory() {
        Page<Meeting> meetingPage = new PageImpl<>(Collections.singletonList(meeting), pageable, 1);

        when(meetingRepository.findByCategory_Name(category.getName(), pageable)).thenReturn(meetingPage);

        Page<MeetingResponseDto> result = meetingService.getMeetingsByCategoryName(user.getEmail(), category.getName(), pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("산악회", result.getContent().get(0).getMeetingName());

        verify(meetingRepository, times(1)).findByCategory_Name(category.getName(), pageable);
    }

    @Test
    void getAllMeetingsByParticipantCount_success() {
        Page<Meeting> meetingPage = new PageImpl<>(Collections.singletonList(meeting), pageable, 1);

        when(meetingRepository.findAllByParticipantCountAndDateAfterToday(pageable)).thenReturn(meetingPage);

        Page<MeetingResponseDto> result = meetingService.getAllMeetingsByParticipantCount(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("산악회", result.getContent().get(0).getMeetingName());

        verify(meetingRepository, times(1)).findAllByParticipantCountAndDateAfterToday(pageable);
    }


    //DELETE
    @Test
    void deleteMeeting_success_byLeader() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(leader));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.existsById(1L)).thenReturn(true);

        meetingService.deleteMeeting("test@example.com", 1L);

        verify(meetingRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteMeeting_success_byAdmin() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.existsById(1L)).thenReturn(true);

        meetingService.deleteMeeting("admin@example.com", 1L);

        verify(meetingRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteMeeting_ThrowsExceptionIfUserNotLeaderOrAdmin() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.existsById(1L)).thenReturn(true);

        ServiceLogicException exception = assertThrows(ServiceLogicException.class, () -> {
            meetingService.deleteMeeting("user@example.com", 1L);
        });

        assertEquals(ExceptionCode.USER_NOT_LEADER, exception.getExceptionCode());
        verify(meetingRepository, never()).deleteById(anyLong());
    }

    @Test
    void joinMeeting_success() {
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findMeetingsByUserId(2L)).thenReturn(Collections.emptyList());
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Participant participant = meetingService.joinMeeting(1L, "user@example.com");

        assertNotNull(participant);
        assertEquals(user, participant.getUser());
        assertEquals(meeting, participant.getMeeting());
        assertFalse(participant.isLeader());
        assertTrue(meeting.getParticipant().contains(participant));
    }

    @Test
    void joinMeeting_ThrowsExceptionIfParticipatingOnSameDate() {
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findMeetingsByUserId(2L)).thenReturn(List.of(meeting));

        assertThrows(ServiceLogicException.class, () -> meetingService.joinMeeting(1L, "user@example.com"));
    }
    @Test
    void endMeeting_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(leader));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(participantsDtoMapper.toDtoList(meeting.getParticipant())).thenReturn(participantDtos);

        List<ParticipantDto> result = meetingService.endMeeting("test@example.com", 1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(meetingRepository, times(1)).save(meeting);
        verify(userChallengeService, times(1)).updateUserChallengeOnMeetingJoin(1L, 1L);
    }

    @Test
    void endMeeting_ThrowsExceptionIfUserNotLeader() {
        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(user));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));

        ServiceLogicException exception = assertThrows(ServiceLogicException.class, () -> {
            meetingService.endMeeting("user@email.com", 1L);
        });

        assertEquals(ExceptionCode.USER_NOT_LEADER, exception.getExceptionCode());
    }

    @Test
    void endMeeting_ThrowsExceptionIfAlreadyEnded() {
        meeting.setEnd(true);

        when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(leader));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));

        ServiceLogicException exception = assertThrows(ServiceLogicException.class, () -> {
            meetingService.endMeeting("test@email.com", 1L);
        });

        assertEquals(ExceptionCode.MEETING_ALREADY_END, exception.getExceptionCode());
    }

}
