package com.example.santa.domain.mountain.service;

import com.example.santa.domain.mountain.dto.MountainResponseDto;
import com.example.santa.domain.mountain.entity.Mountain;
import com.example.santa.domain.mountain.repository.MountainRepository;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.example.santa.global.exception.ServiceLogicException;
import com.example.santa.global.util.mapsturct.MountainResponseDtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class MountainServiceImplTest {

    @Mock
    private MountainRepository mountainRepository;

    @Mock
    private MountainResponseDtoMapper mountainResponseDtoMapper;

    @InjectMocks
    private MountainServiceImpl mountainService;

    private Mountain mountain;
    private MountainResponseDto dto;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        mountain = new Mountain();
        mountain.setId(1L);
        mountain.setName("관악산");
        mountain.setLocation("서울시 관악구");
        mountain.setHeight(8848.86);
        mountain.setDescription("관악에있는 관악산");
        mountain.setPoint("서울쪽에서 정상에 오르는 최단코스");
        mountain.setTransportation("지하철");
        mountain.setLongitude(86.9250);
        mountain.setLatitude(27.9881);

        dto = new MountainResponseDto();
        dto.setId(mountain.getId());
        dto.setName(mountain.getName());
        dto.setLocation(mountain.getLocation());
        dto.setHeight(mountain.getHeight());
        dto.setDescription(mountain.getDescription());
        dto.setPoint(mountain.getPoint());
        dto.setTransportation(mountain.getTransportation());
        dto.setLongitude(mountain.getLongitude());
        dto.setLatitude(mountain.getLatitude());

        pageable = mock(Pageable.class);
    }

    @Test
    void findAllMountains_Success() {
        List<Mountain> mountainList = List.of(mountain);
        Page<Mountain> mountainPage = new PageImpl<>(mountainList);
        when(mountainRepository.findAll(pageable)).thenReturn(mountainPage);
        when(mountainResponseDtoMapper.toDto(mountain)).thenReturn(dto);

        Page<MountainResponseDto> result = mountainService.findAllMountains(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(dto, result.getContent().get(0));
        verify(mountainRepository).findAll(pageable);
        verify(mountainResponseDtoMapper).toDto(mountain);
    }
    @Test
    void findMountainById_Success() {
        when(mountainRepository.findById(anyLong())).thenReturn(Optional.of(mountain));
        when(mountainResponseDtoMapper.toDto(mountain)).thenReturn(dto);

        MountainResponseDto result = mountainService.findMountainById(anyLong());

        assertNotNull(result);
        System.out.println(result.getDescription());
        verify(mountainRepository).findById(anyLong());
        verify(mountainResponseDtoMapper).toDto(mountain);
    }

    @Test
    void findMountainById_ThrowsExceptionIfNotFound() {
        when(mountainRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ServiceLogicException.class, () -> mountainService.findMountainById(anyLong()));
        verify(mountainRepository).findById(anyLong());
    }
}