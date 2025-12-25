package com.collab.collab_editor_backend.service;

import com.collab.collab_editor_backend.entity.Survey;
import com.collab.collab_editor_backend.util.Result;
import java.util.Map;

public interface SurveyService {
    Result<?> submitSurvey(Long userId, Integer score, String comment);
    Result<Map<String, Object>> getSurveyStats();
}
