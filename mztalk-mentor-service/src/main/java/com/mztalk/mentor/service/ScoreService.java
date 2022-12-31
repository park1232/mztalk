package com.mztalk.mentor.service;

import com.mztalk.mentor.domain.dto.ScoreResDto;
import com.mztalk.mentor.domain.dto.ScoreMenteeDto;
import com.mztalk.mentor.domain.dto.ScoreModifyDto;
import com.mztalk.mentor.domain.dto.ScoreReqDto;

import java.util.List;

public interface ScoreService {
    Long save(ScoreReqDto scoreReqDto);

    ScoreResDto findById(Long id);

    List<ScoreResDto> findAll();

    Long deleteScore(Long id);

    Long updateScore(Long id, ScoreModifyDto scoreModifyDto);

    List<ScoreResDto> findScoresByNickname(String nickname);

    boolean isExist(Long userId, Long boardId);

    List<ScoreMenteeDto> findByUserId(Long userId);

    List<ScoreResDto> findByMentorId(Long mentorId);
}
