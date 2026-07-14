package com.school.hei.repository;

import com.school.hei.model.Image;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, UUID> {}
