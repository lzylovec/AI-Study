package com.study.kgraph.mapper;

import com.study.kgraph.entity.Relation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RelationMapper {
    void insertBatch(@Param("list") List<Relation> list);
    List<Relation> findByUserId(@Param("userId") Long userId);
    List<Relation> findByUserIdAndCategory(@Param("userId") Long userId, @Param("category") String category);
    void deleteByUserId(Long userId);
    void deleteByUserIdAndCategory(@Param("userId") Long userId, @Param("category") String category);
    int updateCategory(@Param("userId") Long userId, @Param("oldName") String oldName, @Param("newName") String newName);
}
