package com.study.kgraph.controller;

import com.study.kgraph.entity.Category;
import com.study.kgraph.mapper.CategoryMapper;
import com.study.kgraph.mapper.NoteMapper;
import com.study.kgraph.mapper.ConceptMapper;
import com.study.kgraph.mapper.RelationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private NoteMapper noteMapper;
    @Autowired
    private ConceptMapper conceptMapper;
    @Autowired
    private RelationMapper relationMapper;

    private Long requireUserId(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }

    @GetMapping
    public Object list(HttpSession session) {
        Long userId = requireUserId(session);
        if (userId == null)
            return Collections.emptyList();

        // Sync categories from notes if needed?
        // For now, assume categories table is the source of truth.
        return categoryMapper.findByUserId(userId);
    }

    @PostMapping
    public Object create(HttpSession session, @RequestParam String name) {
        Long userId = requireUserId(session);
        if (userId == null)
            return error("未登录");
        if (name == null || name.trim().isEmpty())
            return error("名称不能为空");

        String cleanName = name.trim();
        Category existing = categoryMapper.findByUserIdAndName(userId, cleanName);
        if (existing != null)
            return error("分类已存在");

        Category c = new Category();
        c.setUserId(userId);
        c.setName(cleanName);
        categoryMapper.insert(c);
        return c;
    }

    @PostMapping("/update")
    public Object update(HttpSession session, @RequestParam Long id, @RequestParam String name) {
        Long userId = requireUserId(session);
        if (userId == null)
            return error("未登录");
        if (name == null || name.trim().isEmpty())
            return error("名称不能为空");

        Category c = categoryMapper.findById(id);
        if (c == null || !c.getUserId().equals(userId))
            return error("分类不存在");

        String oldName = c.getName();
        String newName = name.trim();

        if (oldName.equals(newName))
            return c;

        Category existing = categoryMapper.findByUserIdAndName(userId, newName);
        if (existing != null)
            return error("分类名已存在");

        // Update category table
        categoryMapper.updateName(id, newName);

        // Batch update notes, concepts, relations
        noteMapper.updateCategory(userId, oldName, newName);
        conceptMapper.updateCategory(userId, oldName, newName);
        relationMapper.updateCategory(userId, oldName, newName);

        return c;
    }

    @PostMapping("/delete")
    public Object delete(HttpSession session, @RequestParam Long id) {
        Long userId = requireUserId(session);
        if (userId == null)
            return error("未登录");

        Category c = categoryMapper.findById(id);
        if (c == null || !c.getUserId().equals(userId))
            return error("分类不存在");

        String categoryName = c.getName();

        // Delete from categories
        categoryMapper.delete(id);

        // Update notes to NULL/Empty (or Uncategorized)
        noteMapper.removeCategory(userId, categoryName);

        // For graph data, we can either delete or move.
        // Since graph data is derived, deleting it for consistency is safer than
        // leaving orphaned nodes.
        conceptMapper.deleteByUserIdAndCategory(userId, categoryName);
        relationMapper.deleteByUserIdAndCategory(userId, categoryName);

        return Collections.singletonMap("ok", true);
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> m = new HashMap<>();
        m.put("error", msg);
        return m;
    }
}
