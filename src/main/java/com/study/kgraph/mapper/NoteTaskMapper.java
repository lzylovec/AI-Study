package com.study.kgraph.mapper;

import com.study.kgraph.entity.NoteTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NoteTaskMapper {
    void insert(NoteTask t);

    void updateStatus(@Param("taskId") String taskId, @Param("status") String status);

    NoteTask findByTaskId(@Param("taskId") String taskId);

    NoteTask findByNoteId(@Param("noteId") Long noteId);

    java.util.List<NoteTask> findAll();
}
