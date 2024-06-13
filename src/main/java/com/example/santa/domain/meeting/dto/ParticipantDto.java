package com.example.santa.domain.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDto {
    @Schema(description = "테스트 값", example = "1")
    private Long userId;
    @Schema(description = "테스트 값", example = "test@email.com")
    private String userEmail;
    @Schema(description = "테스트 값", example = "나정균")
    private String userName;
    @Schema(description = "테스트 값", example = "dotseven")
    private String userNickname;
    private String userImage;


}
