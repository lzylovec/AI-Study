package com.study.kgraph.mapper;

import com.study.kgraph.entity.Note;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface NoteMapper {
    void insert(Note note);

    List<Note> findByUserId(@Param("userId") Long userId);

    Note findById(@Param("id") Long id);

    List<Note> search(@Param("userId") Long userId, @Param("keyword") String keyword);

    void updateText(@Param("id") Long id, @Param("text") String text);

    void updateSummary(@Param("id") Long id, @Param("summary") String summary);

    void updateAll(@Param("id") Long id,
                   @Param("title") String title,
                   @Param("text") String text,
                   @Param("summary") String summary,
                   @Param("audioPath") String audioPath);

    void delete(@Param("id") Long id);
}
