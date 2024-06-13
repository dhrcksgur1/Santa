package com.example.santa.domain.userchallenge.service;

import com.example.santa.domain.category.entity.Category;
import com.example.santa.domain.user.entity.User;

public interface UserChallengeService {
    void updateProgress(String email, Long userMountainId);

    void updateUserChallengeOnMeetingJoin(String email, Long meetingId);

    }
