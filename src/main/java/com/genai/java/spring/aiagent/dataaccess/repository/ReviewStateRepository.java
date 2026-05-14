package com.genai.java.spring.aiagent.dataaccess.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.genai.java.spring.aiagent.dto.ReviewState;

public interface ReviewStateRepository {

    String REVIEW_STATE_SNAPSHOT_UPSERT_SQL = """
            INSERT INTO review_state_snapshot
            (id, status, checkpoint, file_name, report_markdown, error_message, prompt_snapshot, 
             prompt_history, plan_json, pending_approval, approval_history, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?)
            ON CONFLICT (id) DO UPDATE
              SET status = excluded.status,
                  checkpoint = excluded.checkpoint,
                  file_name = excluded.file_name,
                  report_markdown = excluded.report_markdown,
                  error_message = excluded.error_message,
                  prompt_snapshot = excluded.prompt_snapshot,
                  prompt_history = excluded.prompt_history,
                  plan_json = excluded.plan_json,
                  pending_approval = excluded.pending_approval,
                  approval_history = excluded.approval_history,
                  updated_at = excluded.updated_at
            """;

    String REVIEW_STATE_SNAPSHOT_SELECT_SQL = """
            SELECT id, status, checkpoint, file_name, report_markdown, error_message,
                   prompt_snapshot, prompt_history, plan_json, pending_approval, approval_history, 
                   created_at, updated_at
            FROM review_state_snapshot
            WHERE id = ?
            """;

    int upsertReviewState(ReviewState state) throws JsonProcessingException;

    ReviewState getReviewStateById(String id);
}
