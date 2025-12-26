package com.collab.collab_editor_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.collab.collab_editor_backend.entity.Survey;
import com.collab.collab_editor_backend.entity.User;
import com.collab.collab_editor_backend.mapper.SurveyMapper;
import com.collab.collab_editor_backend.mapper.UserMapper;
import com.collab.collab_editor_backend.service.SurveyService;
import com.collab.collab_editor_backend.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SurveyServiceImpl implements SurveyService {

    @Autowired
    private SurveyMapper surveyMapper;

    @Autowired
    private UserMapper userMapper;

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
        Map<String, Long> scoreDistribution = new HashMap<>();

        if (!surveys.isEmpty()) {
            averageScore = surveys.stream()
                .mapToInt(Survey::getScore)
                .average()
                .orElse(0.0);
            
            // 计算评分分布
            scoreDistribution = surveys.stream()
                .collect(Collectors.groupingBy(s -> String.valueOf(s.getScore()), Collectors.counting()));
        }

        // 获取最近的5条评论
        List<Survey> recentSurveys = surveyMapper.selectList(
            new QueryWrapper<Survey>()
                .orderByDesc("create_time")
                .last("LIMIT 5")
        );
        
        // 转换为包含用户名的 Map 列表
        List<Map<String, Object>> commentsWithUser = recentSurveys.stream().map(survey -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", survey.getId());
            map.put("userId", survey.getUserId());
            map.put("score", survey.getScore());
            map.put("comment", survey.getComment());
            map.put("createTime", survey.getCreateTime());
            
            if (survey.getUserId() != null) {
                User user = userMapper.selectById(survey.getUserId());
                map.put("username", user != null ? user.getUsername() : "Unknown");
            } else {
                map.put("username", "Anonymous");
            }
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", surveys.size());
        stats.put("averageScore", averageScore);
        stats.put("scoreDistribution", scoreDistribution);
        stats.put("recentComments", commentsWithUser);
        stats.put("comments", commentsWithUser); // Alias for SurveyStats

        return Result.success(stats);
    }
}
