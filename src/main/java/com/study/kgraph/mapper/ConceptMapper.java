package com.study.kgraph.mapper;

import com.study.kgraph.entity.Concept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ConceptMapper {
    void insertBatch(@Param("list") List<Concept> list);
    List<Concept> findByUserId(@Param("userId") Long userId);
}
