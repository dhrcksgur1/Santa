package com.example.santa.domain.meeting.service;

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
import com.example.santa.global.constant.Constants;
import com.example.santa.global.exception.ExceptionCode;
import com.example.santa.global.exception.ServiceLogicException;
import com.example.santa.global.util.S3ImageService;
import com.example.santa.global.util.mapsturct.ParticipantsDtoMapper;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MeetingServiceImpl implements MeetingService {

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final MeetingTagRepository meetingTagRepository;
    private final ParticipantRepository participantRepository;
    private final ParticipantsDtoMapper participantsDtoMapper;
    private final S3ImageService s3ImageService;
    private final UserChallengeService userChallengeService;

    public MeetingServiceImpl(MeetingRepository meetingRepository, UserRepository userRepository, CategoryRepository categoryRepository, TagRepository tagRepository, MeetingTagRepository meetingTagRepository, ParticipantRepository participantRepository, ParticipantsDtoMapper participantsDtoMapper, S3ImageService s3ImageService, UserChallengeService userChallengeService) {
        this.meetingRepository = meetingRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.meetingTagRepository = meetingTagRepository;
        this.participantRepository = participantRepository;
        this.participantsDtoMapper = participantsDtoMapper;
        this.s3ImageService = s3ImageService;
        this.userChallengeService = userChallengeService;
    }

    @Override
    public MeetingResponseDto createMeeting(String email, MeetingDto meetingDto){
        // dto에서 불러온 카테고리명으로 카테고리를 가져옴
        Category category = categoryRepository.findByName(meetingDto.getCategoryName())
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.CATEGORY_NOT_FOUND));
        // 현재 로그인 한 유저를 불러옴
        User leader = userRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.USER_NOT_FOUND));

        // 이미 같은 날짜에 다른 모임에 참여 중인지 확인
        boolean isParticipatingOnSameDate = userRepository.findMeetingsByUserId(leader.getId()).stream()
                .anyMatch(m -> m.getDate().equals(meetingDto.getDate()));

        if (isParticipatingOnSameDate) {
            // 같은 날짜에 다른 모임에 이미 참여중인 경우 예외 발생
            throw new ServiceLogicException(ExceptionCode.ALREADY_PARTICIPATING_ON_DATE);
        }

        MultipartFile imageFile = meetingDto.getImageFile();
        String imageUrl = Constants.DEFAULT_URL + "meeting_default_image.png";
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = s3ImageService.upload(imageFile);
        }


        Meeting meeting = Meeting.builder()
                .meetingName(meetingDto.getMeetingName())
                .leader(leader)
                .category(category)
                .mountainName(meetingDto.getMountainName())
                .description(meetingDto.getDescription())
                .headcount(meetingDto.getHeadcount())
                .date(meetingDto.getDate())
                .image(imageUrl)
                .build();

        meetingRepository.save(meeting);

        Set<MeetingTag> meetingTags = new HashSet<>();
        // dto에 있는 해시태그에서 생성되지 않은 해시태그면 생성해주고 생성되어 있으면 가져옴
        for (String tagName : meetingDto.getTags()) {
            String saveTagName = tagName.replace(" ", "");
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(Tag.builder()
                            .name(saveTagName)
                            .build()));
            MeetingTag meetingTag = MeetingTag.builder()
                    .tag(tag)
                    .meeting(meeting)
                    .build();
            meetingTags.add(meetingTagRepository.save(meetingTag));
        }

        meeting.setMeetingTags(meetingTags);

        meetingRepository.save(meeting);
        // 모임장을 참가자 목록에 추가해줌
        Participant participant = Participant.builder()
                .user(leader)
                .meeting(meeting)
                .isLeader(true)
                .build();
        List<Participant> participants = new ArrayList<>();
        participants.add(participantRepository.save(participant));

        meeting.setParticipant(participants);

        return convertToDto(meetingRepository.save(meeting));

    }
    @Override
    public MeetingResponseDto meetingDetail(Long id){
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.MEETING_NOT_FOUND));
        return convertToDto(meeting);
    }

    @Override
    public Participant joinMeeting(Long id, String userEmail) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.MEETING_NOT_FOUND));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.USER_NOT_FOUND));

        // 이미 참여중인지 확인
        boolean isAlreadyParticipant = meeting.getParticipant().stream()
                .anyMatch(participant -> participant.getUser().getId().equals(user.getId()));

        if (isAlreadyParticipant) {
            // 이미 참여중인 경우 예외 발생 또는 적절한 처리
            throw new ServiceLogicException(ExceptionCode.ALREADY_PARTICIPATING);
        }

        // 이미 같은 날짜에 다른 모임에 참여 중인지 확인
        boolean isParticipatingOnSameDate = userRepository.findMeetingsByUserId(user.getId()).stream()
                .anyMatch(m -> m.getDate().equals(meeting.getDate()));

        if (isParticipatingOnSameDate) {
            // 같은 날짜에 다른 모임에 이미 참여중인 경우 예외 발생
            throw new ServiceLogicException(ExceptionCode.ALREADY_PARTICIPATING_ON_DATE);
        }

        Participant participant = Participant.builder()
                .user(user)
                .meeting(meeting)
                .isLeader(false)
                .build();
        List<Participant> participants = meeting.getParticipant();
        participants.add(participantRepository.save(participant));

        return participant;
    }

    @Override
    public Page<MeetingResponseDto> getAllMeetings(Pageable pageable){

        Page<Meeting> meetings = meetingRepository.findAll(pageable);
        return meetings.map(this::convertToDto);

    }

    @Override
    @Transactional
    public MeetingResponseDto updateMeeting(String email, Long id, MeetingDto meetingDto) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.MEETING_NOT_FOUND));
        Category category = categoryRepository.findByName(meetingDto.getCategoryName())
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.CATEGORY_NOT_FOUND));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.USER_NOT_FOUND));

        if (!Objects.equals(user.getId(), meeting.getLeader().getId())){
            throw new ServiceLogicException(ExceptionCode.USER_NOT_LEADER2);
        }

        // 이미 같은 날짜에 다른 모임에 참여 중인지 확인
        boolean isParticipatingOnSameDate = userRepository.findMeetingsByUserId(user.getId()).stream()
                .anyMatch(m -> m.getDate().equals(meetingDto.getDate()));

        if (isParticipatingOnSameDate) {
            // 같은 날짜에 다른 모임에 이미 참여중인 경우 예외 발생
            throw new ServiceLogicException(ExceptionCode.ALREADY_PARTICIPATING_ON_DATE);
        }

        MultipartFile imageFile = meetingDto.getImageFile();
        String imageUrl = meetingDto.getImage();
        System.out.println(imageUrl);

        if (imageFile != null && !imageFile.isEmpty()) {
            if(!Objects.equals(imageUrl, Constants.DEFAULT_URL + "meeting_default_image.png")){
                System.out.println(imageUrl);
                System.out.println("이미지 다름");
                s3ImageService.deleteImageFromS3(imageUrl);
            }
            imageUrl = s3ImageService.upload(imageFile);
        }

        meeting.setMeetingName(meetingDto.getMeetingName());
        meeting.setCategory(category);
        meeting.setMountainName(meetingDto.getMountainName());
        meeting.setDescription(meetingDto.getDescription());
        meeting.setHeadcount(meetingDto.getHeadcount());
        meeting.setDate(meetingDto.getDate());
        meeting.setImage(imageUrl);

        meetingRepository.save(meeting);

        meeting.getMeetingTags().clear();
        meetingTagRepository.deleteByMeeting(meeting);
        for (String tagName : meetingDto.getTags()) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(Tag.builder()
                            .name(tagName)
                            .build()));
            MeetingTag meetingTag = MeetingTag.builder()
                    .tag(tag)
                    .meeting(meeting)
                    .build();
            meeting.getMeetingTags().add(meetingTag);
        }

        meetingTagRepository.saveAll(meeting.getMeetingTags());

        return convertToDto(meetingRepository.save(meeting));
    }

    @Override
    public void deleteMeeting(String email, Long id) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.USER_NOT_FOUND));
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.MEETING_NOT_FOUND));

        if (!meetingRepository.existsById(id)) {
            throw new ServiceLogicException(ExceptionCode.MEETING_NOT_FOUND);
        }

        if (Objects.equals(user.getId(), meeting.getLeader().getId()) || user.getRole() == Role.ADMIN){
            meetingRepository.deleteById(id);
        }
        else{
            throw new ServiceLogicException(ExceptionCode.USER_NOT_LEADER);
        }

    }

    @Override
    public Page<MeetingResponseDto> getMeetingsByTagName(String tagName, Pageable pageable) {
        Page<Meeting> meetings = meetingRepository.findByMeetingTags_Tag_NameContaining(tagName,pageable);
        return meetings.map(this::convertToDto);
    }



    @Override
    public Page<MeetingResponseDto> getMeetingsByCategoryName(String email, String categoryName, Pageable pageable) {
        Page<Meeting> meetings;
        System.out.println(email);
        System.out.println(categoryName);
        if(Objects.equals(categoryName, "맞춤추천")){
            meetings = meetingRepository.findByUserEmailAndPreferredCategories(email,pageable); //선호 카테고리
        }
        else {
            meetings = meetingRepository.findByCategory_Name(categoryName,pageable);
        }

        return meetings.map(this::convertToDto);
    }


    @Override
    public Page<MeetingResponseDto> getAllMeetingsByParticipantCount(Pageable pageable) {
        Page<Meeting> meetings = meetingRepository.findAllByParticipantCountAndDateAfterToday(pageable);
        return meetings.map(this::convertToDto);
    }

    public Page<MeetingResponseDto> getAllMeetingsByPreferredCategory(String email, Pageable pageable) {
        Page<Meeting> meetings = meetingRepository.findByUserEmailAndPreferredCategories(email,pageable);
        return meetings.map(this::convertToDto);
    }



    @Override
    public Page<MeetingResponseDto> getMyMeetings(String email, Pageable pageable){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.USER_NOT_FOUND));

        Page<Meeting> meetings = meetingRepository.findMeetingsByParticipantUserId(user.getId(), pageable);
        return meetings.map(this::convertToDto);
    }



    @Override
    public List<ParticipantDto> endMeeting(String email, Long id) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.USER_NOT_FOUND));
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ServiceLogicException(ExceptionCode.MEETING_NOT_FOUND));

        if (!Objects.equals(user.getId(), meeting.getLeader().getId())){
            throw new ServiceLogicException(ExceptionCode.USER_NOT_LEADER);
        }

        if(meeting.isEnd()){
            throw new ServiceLogicException(ExceptionCode.MEETING_ALREADY_END);
        }
        meeting.setEnd(true);

        meetingRepository.save(meeting);

        if(!Objects.equals(meeting.getCategory().getName(), "기타")){
            for (ParticipantDto participant : participantsDtoMapper.toDtoList(meeting.getParticipant())){
                userChallengeService.updateUserChallengeOnMeetingJoin(participant.getUserEmail(),id);
            }
        }
        return participantsDtoMapper.toDtoList(meeting.getParticipant());

    }

    public MeetingResponseDto convertToDto(Meeting meeting) {
        MeetingResponseDto meetingDto = new MeetingResponseDto();
        meetingDto.setMeetingId(meeting.getId());
        meetingDto.setMeetingName(meeting.getMeetingName()); // 모임 이름 설정
        meetingDto.setCategoryName(meeting.getCategory().getName()); // 카테고리 이름 설정
        meetingDto.setMountainName(meeting.getMountainName()); // 산 이름 설정
        meetingDto.setDescription(meeting.getDescription()); // 설명 설정
        meetingDto.setHeadcount(meeting.getHeadcount()); // 인원 수 설정
        meetingDto.setDate(meeting.getDate()); // 날짜 설정
        meetingDto.setImage(meeting.getImage()); // 이미지 설정

        // 태그 설정
        List<String> tags = meeting.getMeetingTags().stream()
                .map(meetingTag -> meetingTag.getTag().getName())
                .collect(Collectors.toList());
        meetingDto.setTags(tags);

        List<Participant> participants = meeting.getParticipant(); // Meeting에서 참가자 목록을 가져옵니다.
        List<ParticipantDto> participantDtoList = new ArrayList<>(); // ParticipantDto 객체를 저장할 리스트를 생성합니다.

        for (Participant participant : participants) {
            User user = participant.getUser(); // 각 Participant에서 User 엔티티를 가져옵니다.

            // ParticipantDto 객체를 생성하고, User 정보를 설정합니다.
            ParticipantDto participantDto = new ParticipantDto();
            participantDto.setUserId(user.getId());
            participantDto.setUserName(user.getName());
            participantDto.setUserNickname(user.getNickname());
            participantDto.setUserImage(user.getImage());

            // 생성한 ParticipantDto 객체를 리스트에 추가합니다.
            participantDtoList.add(participantDto);

            // 참가자가 모임장이면 모임장 추가
            if(participant.isLeader()){
                meetingDto.setLeaderId(user.getId());
            }
        }

        // ParticipantDto 리스트를 MeetingDto 객체의 participants로 설정합니다.
        meetingDto.setParticipants(participantDtoList);

        return meetingDto;
    }

}
