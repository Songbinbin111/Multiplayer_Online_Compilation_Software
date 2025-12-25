package com.collab.collab_editor_backend.controller;

import com.collab.collab_editor_backend.service.SurveyService;
import com.collab.collab_editor_backend.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/survey")
public class SurveyController {

    @Autowired
    private SurveyService surveyService;

    @PostMapping("/submit")
    public Result<?> submitSurvey(@RequestBody Map<String, Object> payload) {
        // 从payload中获取数据
        // 注意：实际项目中应该使用DTO，这里为了简便直接用Map
        // userId可以从Token中获取，这里简化处理从前端传
        Integer userIdInt = (Integer) payload.get("userId");
        Long userId = userIdInt != null ? Long.valueOf(userIdInt) : null;
        Integer score = (Integer) payload.get("score");
        String comment = (String) payload.get("comment");
        
        return surveyService.submitSurvey(userId, score, comment);
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return surveyService.getSurveyStats();
    }
}
