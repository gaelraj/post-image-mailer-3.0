package com.school.hei.endpoint.event.model;

import java.time.Duration;
import java.util.UUID;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class ImageReceived extends PojaEvent {
  private UUID imageId;
  private String originalS3Key;
  private String email;
  private String fileName;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(60);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }
}
