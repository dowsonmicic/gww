package com.gww.website;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.gww.website.media.config.MediaProperties;
import com.gww.website.media.entity.MediaRecord;
import com.gww.website.media.mapper.MediaMapper;
import com.gww.website.media.service.LocalMediaStore;
import com.gww.website.media.service.MediaService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 模拟数据库失败，确认补偿仅删除本次创建的公开副本。 */
class MediaRollbackTest {
  @TempDir Path directory;

  @Test
  void databaseFailureRemovesNewCopyButConflictPreservesExistingFile() throws Exception {
    var properties = new MediaProperties();
    properties.setPublicRoot(directory.resolve("public").toString());
    properties.setPrivateRoot(directory.resolve("private").toString());
    var store = new LocalMediaStore(properties);
    var saved = store.save(LocalMediaStoreTest.png());
    var mapper = mock(MediaMapper.class);
    var row = new MediaRecord("id", saved.key(), "test.png", "image/png", saved.size(), false);
    when(mapper.lock("id")).thenReturn(row);
    doThrow(new IllegalStateException("database failure")).when(mapper).publish("id");
    var service = new MediaService(mapper, store, properties);
    TransactionSynchronizationManager.initSynchronization();
    try {
      assertThrows(IllegalStateException.class, () -> service.publish("id"));
      for (var sync : TransactionSynchronizationManager.getSynchronizations()) {
        sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
      }
      assertFalse(Files.exists(directory.resolve("public").resolve(saved.key())));
      assertTrue(Files.exists(store.file(saved.key(), false)));
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
    store.publish(saved.key());
    TransactionSynchronizationManager.initSynchronization();
    try {
      assertThrows(
          com.gww.website.common.exception.BusinessException.class, () -> service.publish("id"));
      assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
      assertTrue(Files.exists(store.file(saved.key(), true)));
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }
}
