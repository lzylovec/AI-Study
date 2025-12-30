package com.study.kgraph.controller;

import com.study.kgraph.entity.Material;
import com.study.kgraph.mapper.MaterialMapper;
import com.study.kgraph.service.GraphService;
import com.study.kgraph.service.TikaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.util.Collections;

@RestController
@RequestMapping("/api/material")
public class MaterialController {
  @Autowired
  private MaterialMapper materialMapper;
  @Autowired
  private GraphService graphService;
  @Autowired
  private TikaService tikaService;
  @Value("${upload.root}")
  private String uploadRoot;

  @PostMapping("/upload")
  public Object upload(@RequestParam("file") MultipartFile file, @RequestParam("title") String title,
      HttpSession session) {
    try {
      Long userId = (Long) session.getAttribute("userId");
      if (userId == null)
        return Collections.singletonMap("error", "未登录");
      File dir = new File(uploadRoot, "materials");
      if (!dir.exists())
        dir.mkdirs();
      File dest = new File(dir, System.currentTimeMillis() + "_" + file.getOriginalFilename());
      file.transferTo(dest);
      String text = tikaService.extractText(dest);
      Material m = new Material();
      m.setUserId(userId);
      m.setType(ext(file.getOriginalFilename()));
      m.setTitle(title);
      m.setFilePath(dest.getAbsolutePath());
      m.setExtractedText(text);
      materialMapper.insert(m);
      graphService.clearGraph(userId, GraphService.GRAPH_CATEGORY_ALL);
      return Collections.singletonMap("id", m.getId());
    } catch (Exception e) {
      e.printStackTrace();
      return Collections.singletonMap("error", "上传失败: " + e.getMessage());
    }
  }

  private String ext(String name) {
    int i = name.lastIndexOf('.');
    return i >= 0 ? name.substring(i + 1).toUpperCase() : "";
  }
}
