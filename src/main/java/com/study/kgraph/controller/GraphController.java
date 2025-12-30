package com.study.kgraph.controller;

import com.study.kgraph.service.GraphService;
import com.study.kgraph.entity.Note;
import com.study.kgraph.mapper.NoteMapper;
import com.study.kgraph.entity.Material;
import com.study.kgraph.mapper.MaterialMapper;
import com.study.kgraph.entity.Concept;
import com.study.kgraph.entity.Relation;
import com.study.kgraph.mapper.ConceptMapper;
import com.study.kgraph.mapper.RelationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;
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
  @Autowired
  private ConceptMapper conceptMapper;
  @Autowired
  private RelationMapper relationMapper;

  @GetMapping("/build")
  public Object build(HttpSession session,
      @RequestParam(value = "force", required = false) Integer force,
      @RequestParam(value = "category", required = false) String category) {
    Long userId = (Long) session.getAttribute("userId");
    if (userId == null)
      return Collections.emptyMap();
    String cat = (category == null || category.trim().isEmpty()) ? GraphService.GRAPH_CATEGORY_ALL : category.trim();

    List<Note> notes;
    List<Material> mats;
    if (GraphService.GRAPH_CATEGORY_ALL.equals(cat)) {
      notes = noteMapper.findByUserId(userId);
      mats = materialMapper.findByUserId(userId);
    } else if (GraphService.GRAPH_CATEGORY_UNCATEGORIZED.equals(cat)) {
      notes = noteMapper.findByUserIdUncategorized(userId);
      mats = Collections.emptyList();
    } else {
      notes = noteMapper.findByUserIdAndCategory(userId, cat);
      mats = Collections.emptyList();
    }

    String text = concatText(notes, mats);
    boolean forceRebuild = force != null && force.intValue() == 1;
    Map<String, Object> graph = graphService.getOrBuildGraph(userId, cat, text, forceRebuild);
    return graph;
  }

  @PostMapping("/clear")
  public Object clear(HttpSession session, @RequestParam(value = "category", required = false) String category) {
    Long userId = (Long) session.getAttribute("userId");
    if (userId == null)
      return java.util.Collections.singletonMap("error", "未登录");
    if (category == null || category.trim().isEmpty()) {
      graphService.clearAllGraphs(userId);
    } else {
      graphService.clearGraph(userId, category.trim());
    }
    return java.util.Collections.singletonMap("ok", true);
  }

  @GetMapping("/clear")
  public Object clearGet(HttpSession session, @RequestParam(value = "category", required = false) String category) {
    return clear(session, category);
  }

  @GetMapping("/status")
  public Object status(HttpSession session, @RequestParam(value = "category", required = false) String category) {
    Long userId = (Long) session.getAttribute("userId");
    if (userId == null)
      return java.util.Collections.singletonMap("error", "未登录");
    List<Concept> nodes;
    List<Relation> links;
    if (category == null || category.trim().isEmpty()) {
      nodes = conceptMapper.findByUserId(userId);
      links = relationMapper.findByUserId(userId);
    } else {
      String cat = category.trim();
      nodes = conceptMapper.findByUserIdAndCategory(userId, cat);
      links = relationMapper.findByUserIdAndCategory(userId, cat);
    }
    Map<String, Object> m = new HashMap<>();
    m.put("nodeCount", nodes == null ? 0 : nodes.size());
    m.put("linkCount", links == null ? 0 : links.size());
    return m;
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
