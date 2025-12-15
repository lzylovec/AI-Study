package com.study.kgraph.controller;

import com.study.kgraph.service.GraphService;
import com.study.kgraph.entity.Note;
import com.study.kgraph.mapper.NoteMapper;
import com.study.kgraph.entity.Material;
import com.study.kgraph.mapper.MaterialMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpSession;
import java.util.*;

@RestController
@RequestMapping("/api/graph")
public class GraphController {
  @Autowired
  private GraphService graphService;
  @Autowired
  private NoteMapper noteMapper;
  @Autowired
  private MaterialMapper materialMapper;

  @GetMapping("/build")
  public Object build(HttpSession session) {
    Long userId = (Long) session.getAttribute("userId");
    if (userId == null)
      return Collections.emptyMap();
    List<Note> notes = noteMapper.findByUserId(userId);
    List<Material> mats = materialMapper.findByUserId(userId);
    String text = concatText(notes, mats);
    Map<String, Object> graph = graphService.extractGraph(userId, text);
    return graph;
  }

  private String concatText(List<Note> notes, List<Material> mats) {
    StringBuilder sb = new StringBuilder();
    for (Note n : notes)
      if (n.getText() != null)
        sb.append(n.getText()).append('\n');
    for (Material m : mats)
      if (m.getExtractedText() != null)
        sb.append(m.getExtractedText()).append('\n');
    return sb.toString();
  }
}
