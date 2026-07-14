package com.school.hei.validator;

import java.io.IOException;
import java.util.Set;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageFormatValidator {

  private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
  private static final Tika TIKA = new Tika();

  public boolean isValid(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return false;
    }
    if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
      return false;
    }
    return isRealContentTypeValid(file);
  }

  private boolean isRealContentTypeValid(MultipartFile file) {
    try {
      String detectedType = TIKA.detect(file.getInputStream());
      return ALLOWED_CONTENT_TYPES.contains(detectedType);
    } catch (IOException e) {
      return false;
    }
  }
}
