package com.school.hei.endpoint;

import com.school.hei.dto.request.ImageRequest;
import com.school.hei.dto.response.ImageResponse;
import com.school.hei.endpoint.event.EventProducer;
import com.school.hei.endpoint.event.model.ImageReceived;
import com.school.hei.file.bucket.BucketComponent;
import com.school.hei.model.Image;
import com.school.hei.repository.ImageRepository;
import com.school.hei.validator.ImageFormatValidator;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ImageController {

  private final ImageFormatValidator imageFormatValidator;
  private final BucketComponent bucketComponent;
  private final ImageRepository imageRepository;
  private final EventProducer<ImageReceived> eventProducer;

  @PostMapping("/images")
  @SneakyThrows
  public ResponseEntity<ImageResponse> submit(@ModelAttribute ImageRequest request) {

    if (!imageFormatValidator.isValid(request.getFile())) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    var id = UUID.randomUUID();
    var originalKey = "images/original/" + id;

    var tempFile =
        java.io.File.createTempFile("upload-", "-" + request.getFile().getOriginalFilename());
    request.getFile().transferTo(tempFile);
    bucketComponent.upload(tempFile, originalKey);

    var image =
        new Image(
            id,
            request.getFile().getOriginalFilename(),
            request.getEmail(),
            Instant.now(),
            originalKey,
            null);
    imageRepository.save(image);

    var event =
        ImageReceived.builder()
            .imageId(id)
            .originalS3Key(originalKey)
            .email(request.getEmail())
            .fileName(request.getFile().getOriginalFilename())
            .build();
    eventProducer.accept(List.of(event));

    var response =
        ImageResponse.builder()
            .id(id)
            .fileName(image.getFileName())
            .email(request.getEmail())
            .createdAt(image.getCreatedAt())
            .status("PROCESSING")
            .build();

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/images")
  private ImageResponse toResponseDto(Image image) {
    boolean isReady = image.getBwS3Key() != null;

    String downloadUrl =
        isReady
            ? bucketComponent.presign(image.getBwS3Key(), Duration.ofMinutes(10)).toString()
            : null;

    return ImageResponse.builder()
        .id(image.getId())
        .fileName(image.getFileName())
        .email(image.getEmail())
        .createdAt(image.getCreatedAt())
        .downloadUrl(downloadUrl)
        .status(isReady ? "READY" : "PROCESSING")
        .build();
  }
}
