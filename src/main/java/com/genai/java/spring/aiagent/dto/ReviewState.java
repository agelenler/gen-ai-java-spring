package com.genai.java.spring.aiagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewState {
  private String id;
  @Builder.Default
  private ReviewStatus status = ReviewStatus.QUEUED;

  /** Final Markdown report (present when DONE) */
  private String reportMarkdown;

  /** Uploaded diagram reference (e.g., fileName in your storage) */
  private String fileName;

  /** Xml definition of diagram*/
  private String xml;

  /** Error details (present when ERROR) */
  private String errorMessage;

  @Builder.Default
  private Instant createdAt = Instant.now();

  @Builder.Default
  private Instant updatedAt = Instant.now();

  public void updateStatus(ReviewStatus status) {
    this.status = Objects.requireNonNull(status);
    this.updatedAt = Instant.now();
  }

  public void updateReportMarkdown(String reportMarkdown) {
    this.reportMarkdown = reportMarkdown;
    this.updatedAt = Instant.now();
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    this.updatedAt = Instant.now();
  }
}
