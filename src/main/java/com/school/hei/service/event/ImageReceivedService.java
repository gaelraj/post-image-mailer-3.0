package com.school.hei.service.event;

import com.school.hei.endpoint.event.model.ImageReceived;
import com.school.hei.file.bucket.BucketComponent;
import com.school.hei.mail.Email;
import com.school.hei.mail.Mailer;
import com.school.hei.model.Image;
import com.school.hei.repository.ImageRepository;
import jakarta.mail.internet.InternetAddress;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ImageReceivedService implements Consumer<ImageReceived> {

  private final BucketComponent bucketComponent;
  private final ImageRepository imageRepository;
  private final Mailer mailer;

  @SneakyThrows
  @Override
  public void accept(ImageReceived event) {
    File original = bucketComponent.download(event.getOriginalS3Key());

    File blackAndWhiteFile = convertToBlackAndWhite(original);

    var bwKey = "images/bw/" + event.getImageId();
    bucketComponent.upload(blackAndWhiteFile, bwKey);

    Image image =
        imageRepository
            .findById(event.getImageId())
            .orElseThrow(() -> new RuntimeException("Image not found: " + event.getImageId()));
    image.setBwS3Key(bwKey);
    imageRepository.save(image);

    var downloadLink = bucketComponent.presign(bwKey, Duration.ofDays(7));
    var email =
        new Email(
            new InternetAddress(event.getEmail()),
            List.of(),
            List.of(),
            "Your image is ready",
            "<p>Your image <b>"
                + event.getFileName()
                + "</b> has been processed. "
                + "Download link (valid 7 days): "
                + "<a href=\""
                + downloadLink
                + "\">"
                + downloadLink
                + "</a></p>",
            List.of());
    mailer.accept(email);
  }

  private File convertToBlackAndWhite(File original) throws Exception {
    BufferedImage image = ImageIO.read(original);
    BufferedImage grayImage =
        new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

    var graphics = grayImage.getGraphics();
    graphics.drawImage(image, 0, 0, null);
    graphics.dispose();

    File bwFile = File.createTempFile("bw-", "-" + original.getName());
    var format = original.getName().toLowerCase().endsWith(".png") ? "png" : "jpg";
    ImageIO.write(grayImage, format, bwFile);
    return bwFile;
  }
}
