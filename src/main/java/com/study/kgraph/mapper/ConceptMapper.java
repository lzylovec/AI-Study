package com.study.kgraph.mapper;

import com.study.kgraph.entity.Concept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ConceptMapper {
    void insert(Concept c);
    void insertBatch(@Param("list") List<Concept> list);
    List<Concept> findByUserId(@Param("userId") Long userId);
    List<Concept> findByUserIdAndCategory(@Param("userId") Long userId, @Param("category") String category);
    void deleteByUserId(Long userId);
    void deleteByUserIdAndCategory(@Param("userId") Long userId, @Param("category") String category);
    int updateCategory(@Param("userId") Long userId, @Param("oldName") String oldName, @Param("newName") String newName);
}
