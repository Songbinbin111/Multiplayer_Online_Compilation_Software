package com.collab.collab_editor_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.collab.collab_editor_backend.entity.Survey;
import com.collab.collab_editor_backend.mapper.SurveyMapper;
import com.collab.collab_editor_backend.service.SurveyService;
import com.collab.collab_editor_backend.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SurveyServiceImpl implements SurveyService {

    @Autowired
    private SurveyMapper surveyMapper;

    @Override
    public Result<?> submitSurvey(Long userId, Integer score, String comment) {
        if (score == null || score < 1 || score > 5) {
            return Result.error("评分必须在1-5之间");
        }

        Survey survey = new Survey();
        survey.setUserId(userId);
        survey.setScore(score);
        survey.setComment(comment);
        survey.setCreateTime(LocalDateTime.now());

        surveyMapper.insert(survey);
        return Result.successWithMessage("感谢您的反馈！");
    }

    @Override
    public Result<Map<String, Object>> getSurveyStats() {
        List<Survey> surveys = surveyMapper.selectList(null);
        
        double averageScore = 0;
        if (!surveys.isEmpty()) {
            averageScore = surveys.stream()
                .mapToInt(Survey::getScore)
                .average()
                .orElse(0.0);
        }

        // 获取最近的5条评论
        List<Survey> recentComments = surveyMapper.selectList(
            new QueryWrapper<Survey>()
                .orderByDesc("create_time")
                .last("LIMIT 5")
        );

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", surveys.size());
        stats.put("averageScore", String.format("%.1f", averageScore));
        stats.put("recentComments", recentComments);

        return Result.success(stats);
    }
}
