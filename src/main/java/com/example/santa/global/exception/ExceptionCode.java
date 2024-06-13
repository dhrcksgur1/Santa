package com.example.santa.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ExceptionCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 회원입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 닉네임입니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다."),
    OTHER_CATEGORY_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리이름 입니다."),
    CATEGORY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 카테고리입니다."),
    MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "모임을 찾을 수 없습니다."),
    ALREADY_PARTICIPATING(HttpStatus.CONFLICT, "이미 참여중인 모임입니다" ),
    ALREADY_PARTICIPATING_ON_DATE(HttpStatus.CONFLICT, "참여중인 날짜입니다." ),
    USER_NOT_LEADER(HttpStatus.NOT_FOUND, "모임장과 관리자만 삭제할 수 있습니다."),
    USER_NOT_LEADER2(HttpStatus.NOT_FOUND, "모임장만 수정할 수 있습니다."),
    MOUNTAIN_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 산 입니다."),
    USERMOUNTAIN_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 유저 등산 정보입니다."),
    ALREADY_USERMOUNTAIN_ON_DATE(HttpStatus.CONFLICT, "이미 같은 날에 이 산에 대한 인증이 존재합니다."),
    INVALID_USER_LOCATION(HttpStatus.NOT_FOUND, "인증된 위치에 산을 찾을 수 없습니다."),
    INVALID_JWT_TOKEN(HttpStatus.FORBIDDEN, "알수없는 형식에 JWT token 입니다."),
    EXPIRED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "JWT 토큰의 유효기간이 만료되었습니다"),
    UNSUPPORTED_JWT_TOKEN(HttpStatus.FORBIDDEN, "JWT 가 서버에서 지원하지않는 유형입니다."),
    EMPTY_FILE(HttpStatus.NOT_FOUND, "이미지 파일이 존재하지 않습니다."),
    IO_EXCEPTION_ON_IMAGE_UPLOAD(HttpStatus.CONFLICT, "이미지 업로드에 문제가 발생했습니다."),
    NO_FILE_EXTENTION(HttpStatus.NOT_FOUND, "파일 확장자가 존재하지 않습니다." ),
    INVALID_FILE_EXTENTION(HttpStatus.CONFLICT, "업로드 할 수 없는 확장자입니다." ),
    PUT_OBJECT_EXCEPTION(HttpStatus.CONFLICT, "S3에 이미지를 업로드 할 수 없습니다." ),
    IO_EXCEPTION_ON_IMAGE_DELETE(HttpStatus.CONFLICT, "이미지를 삭제 할 수 없습니다." ),
    REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 신고한 사람입니다." ),
    USER_REPORT_EXIST(HttpStatus.CONFLICT, "신고 내역이 있어 탈퇴할 수 없습니다."),
    MEETING_ALREADY_END(HttpStatus.CONFLICT, "모임이 이미 종료 되었습니다." ),
    USERRANKING_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 유저 랭킹 정보입니다."),
    CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 챌랜지입니다.");

    private final HttpStatus status;
    private final String message;

    ExceptionCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

}
