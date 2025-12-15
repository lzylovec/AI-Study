package com.study.kgraph.mapper;

import com.study.kgraph.entity.Material;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface MaterialMapper {
    void insert(Material material);
    List<Material> findByUserId(@Param("userId") Long userId);
}
