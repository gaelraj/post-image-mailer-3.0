package com.school.hei.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Image {

  @Id private UUID id;

  private String fileName;

  private String email;

  private Instant createdAt;

  private String originalS3Key;

  private String bwS3Key;
}
