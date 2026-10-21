package com.gww.website.media.mapper;

import com.gww.website.media.entity.MediaRecord;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface MediaMapper {
  @Select("SELECT * FROM media_record ORDER BY created_at DESC LIMIT 100")
  List<MediaRecord> list();

  @Select("SELECT * FROM media_record WHERE id=#{id} FOR UPDATE")
  MediaRecord lock(String id);

  @Select("SELECT * FROM media_record WHERE id=#{id}")
  MediaRecord find(String id);

  @Insert(
      "INSERT INTO media_record(id,object_key,original_name,content_type,size_bytes,published)"
          + " VALUES(#{id},#{objectKey},#{originalName},#{contentType},#{sizeBytes},#{published})")
  void insert(MediaRecord row);

  @Update("UPDATE media_record SET published=TRUE WHERE id=#{id}")
  void publish(String id);
}
