package com.school.hei.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResponse {
  private UUID id;
  private String fileName;
  private String email;
  private Instant createdAt;
  private String downloadUrl;
  private String status;
}
