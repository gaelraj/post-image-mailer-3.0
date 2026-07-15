package com.school.hei.endpoint;

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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@AllArgsConstructor
public class ImageController {

  private final ImageFormatValidator imageFormatValidator;
  private final BucketComponent bucketComponent;
  private final ImageRepository imageRepository;
  private final EventProducer<ImageReceived> eventProducer;

  @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @SneakyThrows
  public ResponseEntity<ImageResponse> submit(
      @RequestPart("file") MultipartFile file, @RequestParam("email") String email) {

    if (!imageFormatValidator.isValid(file)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    var id = UUID.randomUUID();
    var originalKey = "images/original/" + id;

    var tempFile = java.io.File.createTempFile("upload-", "-" + file.getOriginalFilename());
    file.transferTo(tempFile);
    bucketComponent.upload(tempFile, originalKey);

    var image = new Image(id, file.getOriginalFilename(), email, Instant.now(), originalKey, null);

    imageRepository.save(image);

    var event =
        ImageReceived.builder()
            .imageId(id)
            .originalS3Key(originalKey)
            .email(email)
            .fileName(file.getOriginalFilename())
            .build();

    eventProducer.accept(List.of(event));

    var response =
        ImageResponse.builder()
            .id(id)
            .fileName(image.getFileName())
            .email(email)
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
