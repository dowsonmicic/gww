package com.gww.website.inquiry.mapper;

import com.gww.website.inquiry.entity.InquiryRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.annotations.*;

@Mapper
public interface InquiryMapper {
  @Insert(
      "INSERT INTO inquiry_record(id,name,email,phone,message,locale,status,country,industry,product,website,company)"
          + " VALUES(#{id},#{name},#{email},#{phone},#{message},#{locale},#{status},#{country},#{industry},#{product},#{website},#{company})")
  void insert(InquiryRecord row);

  /** 列表与导出按同一个数据库提交时间字段查询，结束时间为次日零点。 */
  @Select({
    "<script>SELECT * FROM inquiry_record WHERE 1=1",
    "<if test='start != null'>AND created_at &gt;= #{start}</if>",
    "<if test='end != null'>AND created_at &lt; #{end}</if>",
    "<if test='locale != null'>AND locale=#{locale}</if>",
    "ORDER BY created_at DESC, id DESC</script>"
  })
  List<InquiryRecord> list(
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("locale") String locale);

  @Select("SELECT * FROM inquiry_record WHERE id=#{id}")
  InquiryRecord find(String id);

  /** 导出使用游标逐行读取，不受列表条数限制，也不将全部记录驻留内存。 */
  @Select({
    "<script>SELECT * FROM inquiry_record WHERE 1=1",
    "<if test='start != null'>AND created_at &gt;= #{start}</if>",
    "<if test='end != null'>AND created_at &lt; #{end}</if>",
    "<if test='locale != null'>AND locale=#{locale}</if>",
    "ORDER BY created_at DESC, id DESC</script>"
  })
  @Options(fetchSize = 100)
  Cursor<InquiryRecord> export(
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("locale") String locale);

  @Update("UPDATE inquiry_record SET status=#{status} WHERE id=#{id}")
  int updateStatus(String id, String status);
}
