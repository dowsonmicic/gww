package com.gww.website;

import static org.junit.jupiter.api.Assertions.*;

import com.gww.website.common.exception.BusinessException;
import com.gww.website.media.config.MediaProperties;
import com.gww.website.media.service.LocalMediaStore;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

/** 使用临时目录验证媒体隔离，不触及生产存储。 */
class LocalMediaStoreTest {
  @TempDir Path directory;
  MediaProperties properties;
  LocalMediaStore store;

  static MockMultipartFile png() {
    return new MockMultipartFile(
        "file",
        "test.png",
        "image/png",
        new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 0, 0, 0, 0, 0});
  }

  @BeforeEach
  void setup() {
    properties = new MediaProperties();
    properties.setPublicRoot(directory.resolve("public").toString());
    properties.setPrivateRoot(directory.resolve("private").toString());
    store = new LocalMediaStore(properties);
  }

  @Test
  void uploadIsPrivateUntilPublishedAndConflictDoesNotDelete() throws Exception {
    var saved = store.save(png());
    assertTrue(Files.exists(store.file(saved.key(), false)));
    assertThrows(BusinessException.class, () -> store.file(saved.key(), true));
    store.publish(saved.key());
    byte[] original = Files.readAllBytes(store.file(saved.key(), true));
    assertThrows(BusinessException.class, () -> store.publish(saved.key()));
    assertArrayEquals(original, Files.readAllBytes(store.file(saved.key(), true)));
  }

  @Test
  void rejectsFakeFileAndOversize() {
    assertThrows(
        BusinessException.class,
        () -> store.save(new MockMultipartFile("file", "fake.png", "image/png", new byte[16])));
    properties.setMaxUploadBytes(8);
    assertThrows(BusinessException.class, () -> store.save(png()));
  }

  @Test
  void rejectsUnconfiguredRelativeAndBuildDirectories() {
    properties.setPublicRoot("");
    assertThrows(BusinessException.class, () -> store.save(png()));
    properties.setPublicRoot("relative");
    assertThrows(BusinessException.class, () -> store.save(png()));
    properties.setPublicRoot(directory.resolve(".output/media").toString());
    assertThrows(BusinessException.class, () -> store.save(png()));
  }

  @Test
  void rejectsOverlappingDirectoriesAndTraversal() {
    properties.setPublicRoot(directory.resolve("private/public").toString());
    assertThrows(BusinessException.class, () -> store.save(png()));
    properties.setPublicRoot(directory.resolve("public").toString());
    assertThrows(BusinessException.class, () -> store.file("../secret.png", false));
  }
}
