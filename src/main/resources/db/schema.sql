CREATE TABLE IF NOT EXISTS review_state_snapshot (
  id                UUID PRIMARY KEY,
  status            VARCHAR(50) NOT NULL,           -- QUEUED, RUNNING, PENDING_APPROVAL_*, DONE, ERROR
  checkpoint        VARCHAR(50),                    -- INIT, AFTER_PLAN, BEFORE_DIAGRAM_APPROVAL, ...
  file_name         VARCHAR(255),                   -- original uploaded diagram file name
  report_markdown   TEXT,                           -- final markdown report (when DONE)
  error_message     TEXT,                           -- error details (when ERROR)
  prompt_snapshot   TEXT,                           -- serialized conversation JSON (current)
  prompt_history    JSONB,                          -- map of checkpoint -> prompt snapshot (audit trail)
  plan_json         JSONB,                          -- execution plan
  pending_approval  JSONB,                          -- PendingApproval object when paused
  approval_history  JSONB,                          -- map of approval type -> ApprovalRequestData
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_review_state_status ON review_state_snapshot(status);
CREATE INDEX IF NOT EXISTS idx_review_state_checkpoint ON review_state_snapshot(checkpoint);
CREATE INDEX IF NOT EXISTS idx_review_state_created_at ON review_state_snapshot(created_at);
