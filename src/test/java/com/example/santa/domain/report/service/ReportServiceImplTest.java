package com.example.santa.domain.report.service;

import com.example.santa.domain.report.dto.ReportRequestDto;
import com.example.santa.domain.report.dto.ReportResponseDto;
import com.example.santa.domain.report.entity.Report;
import com.example.santa.domain.report.repository.ReportRepository;
import com.example.santa.domain.user.entity.User;
import com.example.santa.domain.user.repository.UserRepository;
import com.example.santa.global.exception.ServiceLogicException;
import com.example.santa.global.util.mapsturct.ReportResponseDtoMapper;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.Optional;


public class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReportResponseDtoMapper reportResponseDtoMapper;

    @InjectMocks
    private ReportServiceImpl reportService;

    private ReportRequestDto reportRequestDto;
    private ReportResponseDto reportResponseDto;
    private Report report;
    private User reporter;
    private User reportedParticipant;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        reporter = new User();
        reporter.setEmail("reporter@example.com");
        reportedParticipant = new User();
        reportedParticipant.setId(1L);

        report = new Report();
        report.setReporter(reporter);
        report.setReportedParticipant(reportedParticipant);
        report.setReason("Inappropriate behavior");

        reportRequestDto = new ReportRequestDto();
        reportRequestDto.setReason("Inappropriate behavior");
        reportRequestDto.setReportedParticipantId(1L);

        reportResponseDto = new ReportResponseDto();
    }

    @Test
    void testReporting_Success() {
        when(userRepository.findByEmail("reporter@example.com")).thenReturn(Optional.of(reporter));
        when(userRepository.findById(reportRequestDto.getReportedParticipantId())).thenReturn(Optional.of(reportedParticipant));
        when(reportRepository.existsByReporterAndReportedParticipant(reporter, reportedParticipant)).thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenReturn(report);
        when(reportResponseDtoMapper.toDto(any(Report.class))).thenReturn(reportResponseDto);

        ReportResponseDto result = reportService.reporting("reporter@example.com", reportRequestDto);

        assertNotNull(result);

        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void testReporting_UserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        Exception exception = assertThrows(ServiceLogicException.class, () -> {
            reportService.reporting("reporter@example.com", reportRequestDto);
        });

        assertEquals("존재하지 않는 회원입니다.", exception.getMessage());
    }

    @Test
    void testReporting_ReportAlreadyExists() {
        when(userRepository.findByEmail("reporter@example.com")).thenReturn(Optional.of(reporter));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reportedParticipant));
        when(reportRepository.existsByReporterAndReportedParticipant(reporter, reportedParticipant)).thenReturn(true);

        Exception exception = assertThrows(ServiceLogicException.class, () -> {
            reportService.reporting("reporter@example.com", reportRequestDto);
        });

        assertEquals("이미 신고한 사람입니다.", exception.getMessage());
    }

    @Test
    void testGetReports() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Report> page = new PageImpl<>(Arrays.asList(report));
        when(reportRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(reportResponseDtoMapper.toDto(any(Report.class))).thenReturn(reportResponseDto);

        Page<ReportResponseDto> results = reportService.getReports(pageRequest);

        System.out.println(results.getContent());

        assertNotNull(results);
        assertEquals(1, results.getContent().size());
        verify(reportRepository).findAll(pageRequest);
    }

    @Test
    void testDeleteReport() {
        doNothing().when(reportRepository).deleteById(anyLong());
        reportService.deleteReport(1L);
        verify(reportRepository).deleteById(1L);
    }
}