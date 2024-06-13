package com.example.santa.global.util.mapsturct;

import com.example.santa.domain.meeting.dto.ParticipantDto;
import com.example.santa.domain.meeting.entity.Participant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface ParticipantsDtoMapper extends EntityMapper<ParticipantDto, Participant> {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(source = "user.name", target = "userName")
    @Mapping(source = "user.nickname", target = "userNickname")
    @Mapping(source = "user.image", target = "userImage")
    ParticipantDto toDto(Participant participant);

}
