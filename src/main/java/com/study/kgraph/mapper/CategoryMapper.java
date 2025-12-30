package com.study.kgraph.mapper;

import com.study.kgraph.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CategoryMapper {
    int insert(Category category);
    List<Category> findByUserId(Long userId);
    Category findById(@Param("id") Long id);
    Category findByUserIdAndName(@Param("userId") Long userId, @Param("name") String name);
    int updateName(@Param("id") Long id, @Param("name") String name);
    int delete(@Param("id") Long id);
}
