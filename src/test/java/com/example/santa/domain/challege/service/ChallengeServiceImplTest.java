package com.example.santa.domain.challege.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.example.santa.domain.category.entity.Category;
import com.example.santa.domain.category.repository.CategoryRepository;
import com.example.santa.domain.challege.dto.ChallengeCreateDto;
import com.example.santa.domain.challege.dto.ChallengeResponseDto;
import com.example.santa.domain.challege.entity.Challenge;
import com.example.santa.domain.challege.repository.ChallengeRepository;
import com.example.santa.domain.userchallenge.repository.UserChallengeRepository;
import com.example.santa.global.exception.ServiceLogicException;
import com.example.santa.global.util.S3ImageService;
import com.example.santa.global.util.mapsturct.ChallengeResponseMapper;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.assertThat;


import java.util.Arrays;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class ChallengeServiceImplTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserChallengeRepository userChallengeRepository;

    @Mock
    private ChallengeResponseMapper challengeResponseMapper;

    @Mock
    private S3ImageService s3ImageService;

    @InjectMocks
    private ChallengeServiceImpl challengeService;

    private ChallengeCreateDto challengeCreateDto;
    private Challenge challenge;
    private Category category;
    private ChallengeResponseDto challengeResponseDto;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        // Setup data
        category = new Category();
        category.setId(1L);
        category.setName("등산");

        challenge = new Challenge();
        challenge.setId(1L);
        challenge.setName("100대 명산 등반");
        challenge.setDescription("100대 명산을 전부 등반해보세요");
        challenge.setClearStandard(100);
        challenge.setImage("some-url");
        challenge.setCategory(category);

        challengeCreateDto = new ChallengeCreateDto();
        challengeCreateDto.setCategoryName("플로깅");
        challengeCreateDto.setName("100대 명산 등반");
        challengeCreateDto.setDescription("100대 명산을 전부 등반해보세요.");
        challengeCreateDto.setClearStandard(100);
        challengeCreateDto.setImage("some-url");
        challengeCreateDto.setImageFile(null);


        challengeResponseDto = new ChallengeResponseDto();
        challengeResponseDto.setId(challenge.getId());
        challengeResponseDto.setDescription(challenge.getDescription());
        challengeResponseDto.setClearStandard(challenge.getClearStandard());
        challengeResponseDto.setImage(challenge.getImage());
        challengeResponseDto.setName(challenge.getName());
//        challengeResponseDto.setCategory(challenge.getCategory());
        challengeResponseDto.setCategoryName(challenge.getCategory().name);
        lenient().when(challengeResponseMapper.toDto(any(Challenge.class))).thenReturn(challengeResponseDto);

    }

    @Test
    void saveChallenge_WithDefaultImage() {
        challengeCreateDto.setImageFile(null);

        when(categoryRepository.findByName(anyString())).thenReturn(Optional.of(category));
        when(challengeRepository.save(any(Challenge.class))).thenReturn(challenge);
        when(challengeResponseMapper.toDto(challenge)).thenReturn(challengeResponseDto);
        ChallengeResponseDto result = challengeService.saveChallenge(challengeCreateDto);

        assertNotNull(result);
        assertEquals(challengeResponseDto, result);
        verify(s3ImageService, never()).upload(any());
    }

    @Test
    void saveChallenge_WithNewImage() {
        MultipartFile imageFile = new MockMultipartFile("file", "test.png", "image/png", "test image content".getBytes());
        challengeCreateDto.setImageFile(imageFile);
        when(categoryRepository.findByName(anyString())).thenReturn(Optional.of(category));
        when(s3ImageService.upload(any(MultipartFile.class))).thenReturn("uploaded-image-url");
        when(challengeRepository.save(any(Challenge.class))).thenReturn(challenge);
        when(challengeResponseMapper.toDto(challenge)).thenReturn(challengeResponseDto);

        ChallengeResponseDto result = challengeService.saveChallenge(challengeCreateDto);

        System.out.println(challenge.getName());
        System.out.println(result);
        assertNotNull(result);
        assertEquals(challengeResponseDto, result);
        verify(s3ImageService).upload(any(MultipartFile.class));
    }

    @Test
    void findAllChallenges_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Category category = new Category(1L, "힐링");

        Challenge challenge1 = Challenge.builder()
                .id(1L)
                .name("힐링 모임 5회 참여")
                .description("힐링 모임에 5회 참여해보세요!")
                .image("image1.jpg")
                .clearStandard(5)
                .category(category)
                .build();

        Challenge challenge2 = Challenge.builder()
                .id(2L)
                .name("식도락 모임 10회 참여")
                .description("식도락 모임에 10회 참여해보세요!")
                .image("image2.jpg")
                .clearStandard(10)
                .category(category)
                .build();

        Page<Challenge> challengesPage = new PageImpl<>(Arrays.asList(challenge1, challenge2));
        when(challengeRepository.findAll(pageable)).thenReturn(challengesPage);

        ChallengeResponseDto dto1 = new ChallengeResponseDto(1L, "힐링 모임 5회 참여", "힐링 모임에 5회 참여해보세요!", "image1.jpg", 5, "힐링");
        ChallengeResponseDto dto2 = new ChallengeResponseDto(2L, "식도락 모임 10회 참여", "식도락 모임에 10회 참여해보세요!", "image2.jpg", 10, "힐링");

        when(challengeResponseMapper.toDto(challenge1)).thenReturn(dto1);
        when(challengeResponseMapper.toDto(challenge2)).thenReturn(dto2);

        // When
        Page<ChallengeResponseDto> result = challengeService.findAllChallenges(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(ChallengeResponseDto::getId).containsExactlyInAnyOrder(1L, 2L);
        assertThat(result.getContent()).extracting(ChallengeResponseDto::getName).containsExactlyInAnyOrder("힐링 모임 5회 참여", "식도락 모임 10회 참여");
    }



    @Test
    void findChallengeById_NotFound() {
        when(challengeRepository.findById(anyLong())).thenReturn(Optional.empty());

        ChallengeResponseDto result = challengeService.findChallengeById(1L);

        assertNull(result);
    }

    @Test
    void findChallengeById_Found() {

        when(challengeRepository.findById(1L)).thenReturn(Optional.of(challenge));

        ChallengeResponseDto result = challengeService.findChallengeById(1L);

        System.out.println(result);
        System.out.println(challengeResponseDto);
        assertNotNull(result);
        assertEquals(challengeResponseDto, result);
    }

    @Test
    void updateChallenge_WithImage_UpdatesChallengeAndImage() {
        // Given
        Long challengeId = 1L;
        MockMultipartFile imageFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image content".getBytes());
        ChallengeCreateDto requestDto = new ChallengeCreateDto(
                "힐링",
                "Updated Name",
                "Updated Description",
                10,
                null,
                imageFile
        );
        Category category = new Category(1L, "힐링");
        Challenge challenge = new Challenge();
        ChallengeResponseDto responseDto = new ChallengeResponseDto();

        when(categoryRepository.findByName(anyString())).thenReturn(Optional.of(category));
        when(challengeRepository.findById(anyLong())).thenReturn(Optional.of(challenge));
        when(s3ImageService.upload(any(MultipartFile.class))).thenReturn("newImageUrl");
        when(challengeRepository.save(any(Challenge.class))).thenReturn(challenge);
        when(challengeResponseMapper.toDto(any(Challenge.class))).thenReturn(responseDto);

        // When
        ChallengeResponseDto result = challengeService.updateChallenge(challengeId, requestDto);

        // Then
        assertNotNull(result);
        verify(s3ImageService, times(1)).upload(any(MultipartFile.class));
        verify(challengeRepository, times(1)).findById(challengeId);
        verify(challengeRepository, times(1)).save(any(Challenge.class));
    }

    @Test
    void updateChallenge_WithoutImage_UpdatesChallengeInfoOnly() {
        // Given
        Long challengeId = 1L;
        ChallengeCreateDto requestDto = new ChallengeCreateDto(
                "힐링",
                "Updated Name",
                "Updated Description",
                10,
                "existingImageUrl",
                null
        );
        Category category = new Category(1L, "힐링");
        Challenge challenge = new Challenge();
        ChallengeResponseDto responseDto = new ChallengeResponseDto();

        when(categoryRepository.findByName(anyString())).thenReturn(Optional.of(category));
        when(challengeRepository.findById(anyLong())).thenReturn(Optional.of(challenge));
        when(challengeRepository.save(any(Challenge.class))).thenReturn(challenge);
        when(challengeResponseMapper.toDto(any(Challenge.class))).thenReturn(responseDto);

        // When
        ChallengeResponseDto result = challengeService.updateChallenge(challengeId, requestDto);

        // Then
        assertNotNull(result);
        verify(s3ImageService, never()).upload(any(MultipartFile.class));
        verify(challengeRepository, times(1)).findById(challengeId);
        verify(challengeRepository, times(1)).save(any(Challenge.class));
    }

    @Test
    void updateChallenge_ThrowsExceptionIfChallengeNotFound() {
        // GIven
        Long nonExistingId = 999L;
        ChallengeCreateDto dto = new ChallengeCreateDto();
        dto.setCategoryName("ExistingCategory");

        when(challengeRepository.findById(anyLong())).thenReturn(Optional.empty()); // ID가 존재하지 않음을 가정

        // When & Then
        assertThrows(ServiceLogicException.class, () -> {
            challengeService.updateChallenge(nonExistingId, dto);
        });
    }

    @Test
    void updateChallenge_ThrowsExceptionIfCategoryNotFound() {
        // Given
        Long challengeId = 1L;
        ChallengeCreateDto dto = new ChallengeCreateDto();
        dto.setCategoryName("NonExistingCategory"); // 카테고리 이름으로 존재하지 않는 값을 설정

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ServiceLogicException.class, () -> {
            challengeService.updateChallenge(challengeId, dto);}
        );
    }

    @Test
    void deleteChallenge_Success() {
        challengeService.deleteChallenge(1L);
        verify(challengeRepository).deleteById(1L);
    }

    @Test
    void saveChallenge_ThrowsExceptionIfCategoryNotFound() {
        when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());

        Exception exception = assertThrows(ServiceLogicException.class, () -> {
            challengeService.saveChallenge(challengeCreateDto);
        });

        assertEquals("존재하지 않는 카테고리입니다.", exception.getMessage());
    }
}