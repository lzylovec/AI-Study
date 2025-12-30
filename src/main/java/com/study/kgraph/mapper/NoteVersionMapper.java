package com.study.kgraph.mapper;

import com.study.kgraph.entity.NoteVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface NoteVersionMapper {
    void insert(NoteVersion v);

    List<NoteVersion> findMetaByNoteId(@Param("noteId") Long noteId);

    NoteVersion findById(@Param("id") Long id);
}

