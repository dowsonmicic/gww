package com.gww.website.content.mapper;

import com.gww.website.content.entity.ContentRecord;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ContentMapper {
  /** 每个官网媒体位置独立保存，避免并发替换不同位置时覆盖彼此。 */
  @Insert("INSERT INTO content_record(id,kind,name,name_en,category,brand,status,description,description_en,image_id)"
      + " VALUES(#{id},#{kind},#{name},#{nameEn},#{category},#{brand},#{status},#{description},#{descriptionEn},#{imageId})"
      + " ON DUPLICATE KEY UPDATE image_id=#{imageId},updated_at=CURRENT_TIMESTAMP")
  void saveSiteMedia(ContentRecord row);

  @Insert("INSERT INTO content_record(id,kind,name,name_en,category,brand,status,description,description_en)"
      + " VALUES(#{id},#{kind},#{name},#{nameEn},#{category},#{brand},#{status},#{description},#{descriptionEn})"
      + " ON DUPLICATE KEY UPDATE description=#{description},updated_at=CURRENT_TIMESTAMP")
  void saveSettings(ContentRecord row);

  @Select(
      "SELECT * FROM content_record WHERE kind=#{kind} ORDER BY updated_at DESC LIMIT #{limit}"
          + " OFFSET #{offset}")
  List<ContentRecord> list(String kind, int limit, int offset);

  @Select(
      "SELECT * FROM content_record WHERE kind=#{kind} AND status='PUBLISHED' ORDER BY updated_at"
          + " DESC LIMIT 100")
  List<ContentRecord> published(String kind);

  @Select("SELECT * FROM content_record WHERE id=#{id} AND kind=#{kind}")
  ContentRecord find(String id, String kind);

  @Insert(
      "INSERT INTO"
          + " content_record(id,kind,name,name_en,category,brand,status,description,description_en,image_id)"
          + " VALUES(#{id},#{kind},#{name},#{nameEn},#{category},#{brand},#{status},#{description},#{descriptionEn},#{imageId})")
  void insert(ContentRecord row);

  @Update(
      "UPDATE content_record SET"
          + " name=#{name},name_en=#{nameEn},category=#{category},brand=#{brand},status=#{status},description=#{description},description_en=#{descriptionEn},image_id=#{imageId},updated_at=CURRENT_TIMESTAMP"
          + " WHERE id=#{id} AND kind=#{kind}")
  int update(ContentRecord row);

  @Delete("DELETE FROM content_record WHERE id=#{id} AND kind=#{kind}")
  int delete(String id, String kind);

  @Select("SELECT COUNT(*) FROM content_record WHERE image_id=#{id}")
  int mediaReferences(String id);
}
