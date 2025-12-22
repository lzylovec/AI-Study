package com.study.kgraph.controller;

import com.study.kgraph.entity.Note;
import com.study.kgraph.entity.NoteVersion;
import com.study.kgraph.mapper.NoteMapper;
import com.study.kgraph.mapper.NoteVersionMapper;
import com.study.kgraph.service.SpeechService;
import com.study.kgraph.service.FileTransServiceAliyun;
import com.study.kgraph.service.OssService;
import com.study.kgraph.entity.NoteTask;
import com.study.kgraph.mapper.NoteTaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {
  @Autowired
  private NoteMapper noteMapper;
  @Autowired
  private NoteVersionMapper noteVersionMapper;
  @Autowired
  private SpeechService speechService;
  @Autowired
  private FileTransServiceAliyun fileTransService;
  @Autowired
  private OssService ossService;
  @Autowired
  private NoteTaskMapper noteTaskMapper;
  @Autowired
  private com.study.kgraph.service.AiSummaryService aiSummaryService;
  @Autowired
  private com.study.kgraph.service.TikaService tikaService;
  @Autowired
  private com.study.kgraph.service.ExportService exportService;

  @Value("${upload.root}")
  private String uploadRoot;

  private Long requireUserId(HttpSession session) {
    return (Long) session.getAttribute("userId");
  }

  private boolean canAccessNote(Long userId, Note note) {
    return userId != null && note != null && note.getUserId() != null && note.getUserId().equals(userId);
  }

  private void saveVersion(Note note) {
    if (note == null)
      return;
    NoteVersion v = new NoteVersion();
    v.setNoteId(note.getId());
    v.setTitle(note.getTitle());
    v.setText(note.getText());
    v.setSummary(note.getSummary());
    try {
      noteVersionMapper.insert(v);
    } catch (Exception ignored) {
    }
  }

  @PostMapping("/upload-doc")
  public Object uploadDoc(@RequestParam("file") MultipartFile file,
      @RequestParam(value = "title", required = false) String title,
      HttpSession session) {
    try {
      Long userId = requireUserId(session);
      if (userId == null)
        return java.util.Collections.singletonMap("error", "未登录");

      // 1. 保存文件
      File dir = new File(uploadRoot, "docs");
      if (!dir.exists())
        dir.mkdirs();
      File dest = new File(dir, System.currentTimeMillis() + "_" + file.getOriginalFilename());
      file.transferTo(dest);

      // 2. 提取文本
      String text = tikaService.extractText(dest);
      if (text == null || text.trim().isEmpty()) {
        return java.util.Collections.singletonMap("error", "无法从文档中提取文本，或者文档为空");
      }

      // 3. 创建笔记
      Note n = new Note();
      n.setUserId(userId);
      n.setTitle((title != null && !title.isEmpty()) ? title : file.getOriginalFilename());
      n.setText(text);
      n.setAudioPath(dest.getAbsolutePath());
      noteMapper.insert(n);
      saveVersion(n);

      return java.util.Collections.singletonMap("id", n.getId());
    } catch (Exception e) {
      e.printStackTrace();
      return java.util.Collections.singletonMap("error", "文档处理失败: " + e.getMessage());
    }
  }

  @PostMapping("/upload-audio")
  public Object uploadAudio(@RequestParam("file") MultipartFile file, @RequestParam("title") String title,
      HttpSession session) {
    try {
      Long userId = requireUserId(session);
      if (userId == null)
        return java.util.Collections.singletonMap("error", "未登录");
      File dir = new File(uploadRoot, "audio");
      dir.mkdirs();
      File dest = new File(dir, System.currentTimeMillis() + "_" + file.getOriginalFilename());
      file.transferTo(dest);

      // 检查是否为 MP3，如果是则走自动长语音流程
      String name = file.getOriginalFilename();
      if (name != null && name.toLowerCase().endsWith(".mp3")) {
        return processMp3Automatically(file, title, userId, dest);
      }

      String text = speechService.transcribe(dest);
      Note n = new Note();
      n.setUserId(userId);
      n.setTitle(title);
      n.setText(text);
      n.setAudioPath(dest.getAbsolutePath());
      noteMapper.insert(n);
      saveVersion(n);
      return java.util.Collections.singletonMap("id", n.getId());
    } catch (Exception e) {
      String msg = e.getMessage();
      if (msg != null && msg.contains("UserDisable")) {
        msg += " (请检查阿里云OSS服务是否开通或欠费)";
      }
      java.util.Map<String, Object> m = new java.util.HashMap<>();
      m.put("error", "语音识别失败: " + msg);
      return m;
    }
  }

  private Object processMp3Automatically(MultipartFile file, String title, Long userId, File dest) throws Exception {
    // 1. 上传 OSS
    String fileUrl = ossService.uploadAndSign(dest, "audio/");
    if (fileUrl == null || fileUrl.isEmpty())
      throw new RuntimeException("MP3转写需要OSS，上传失败");

    // 2. 提交任务
    String taskId = fileTransService.submit(fileUrl);
    if (taskId == null || taskId.isEmpty())
      throw new RuntimeException("提交转写任务失败");

    // 3. 轮询等待 (最多 60 秒)
    String resultText = null;
    for (int i = 0; i < 60; i++) {
      Thread.sleep(1000);
      FileTransServiceAliyun.Result r = fileTransService.query(taskId);
      System.out.println("Checking Task " + taskId + " Status: " + r.status + " (Attempt " + (i + 1) + ")");

      if ("SUCCESS".equals(r.status)) {
        resultText = parsePlainText(r.resultText);
        break;
      }
      if ("FAILED".equals(r.status)) {
        throw new RuntimeException("转写失败");
      }
      // 如果是 RUNNING 或 QUEUEING，继续等待
    }

    // 4. 保存笔记
    if (resultText == null) {
      // 超时处理：记录一条提示信息
      resultText = "【转写进行中】音频文件正在后台处理，请稍后在控制台日志中查看任务状态，或等待转写完成...";
    }
    Note n = new Note();
    n.setUserId(userId);
    n.setTitle(title);
    n.setAudioPath(dest.getAbsolutePath());
    n.setText(resultText); // 如果超时，这里是 null
    noteMapper.insert(n);
    saveVersion(n);

    // 5. 如果超时未完成，记录 Task 以便后续查询
    if (resultText == null) {
      NoteTask t = new NoteTask();
      t.setNoteId(n.getId());
      t.setTaskId(taskId);
      t.setStatus("RUNNING");
      noteTaskMapper.insert(t);
    }

    return java.util.Collections.singletonMap("id", n.getId());
  }

  @PostMapping("/upload-long-audio")
  public Object uploadLongAudio(@RequestParam("file") MultipartFile file, @RequestParam("title") String title,
      HttpSession session) throws Exception {
    Long userId = requireUserId(session);
    if (userId == null)
      return java.util.Collections.singletonMap("error", "未登录");
    java.io.File dir = new java.io.File(uploadRoot, "audio");
    dir.mkdirs();
    java.io.File dest = new java.io.File(dir, System.currentTimeMillis() + "_" + file.getOriginalFilename());
    file.transferTo(dest);
    String fileUrl = ossService.uploadAndSign(dest, "audio/");
    if (fileUrl == null || fileUrl.isEmpty())
      return java.util.Collections.singletonMap("error", "OSS未配置或上传失败");
    String taskId = fileTransService.submit(fileUrl);
    if (taskId == null || taskId.isEmpty())
      return java.util.Collections.singletonMap("error", "提交识别任务失败");
    Note n = new Note();
    n.setUserId(userId);
    n.setTitle(title);
    n.setAudioPath(dest.getAbsolutePath());
    n.setText(null);
    noteMapper.insert(n);
    saveVersion(n);
    NoteTask t = new NoteTask();
    t.setNoteId(n.getId());
    t.setTaskId(taskId);
    t.setStatus("SUBMITTED");
    noteTaskMapper.insert(t);
    java.util.Map<String, Object> m = new java.util.HashMap<>();
    m.put("noteId", n.getId());
    m.put("taskId", taskId);
    return m;
  }

  @GetMapping("/filetrans-status")
  public Object filetransStatus(@RequestParam String taskId) throws Exception {
    FileTransServiceAliyun.Result r = fileTransService.query(taskId);
    NoteTask t = noteTaskMapper.findByTaskId(taskId);
    if (r.status != null && r.status.equals("SUCCESS") && t != null) {
      String text = parsePlainText(r.resultText);
      if (text != null) {
        Note before = noteMapper.findById(t.getNoteId());
        if (before != null && (before.getText() == null || !before.getText().equals(text))) {
          saveVersion(before);
        }
        noteMapper.updateText(t.getNoteId(), text);
        noteTaskMapper.updateStatus(taskId, "SUCCESS");
      }
    } else if (t != null && r.status != null) {
      noteTaskMapper.updateStatus(taskId, r.status);
    }
    java.util.Map<String, Object> m = new java.util.HashMap<>();
    m.put("status", r.status);
    m.put("result", r.resultText);
    m.put("noteId", t == null ? null : t.getNoteId());
    return m;
  }

  private String parsePlainText(String json) {
    if (json == null || json.isEmpty())
      return null;
    try {
      com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
      com.fasterxml.jackson.databind.JsonNode root = om.readTree(json);
      // 4.0结构："Sentences" 数组或 "Result" 内部字段不同版本；尽量拼接所有文本
      StringBuilder sb = new StringBuilder();
      if (root.has("Sentences")) {
        for (com.fasterxml.jackson.databind.JsonNode s : root.get("Sentences")) {
          com.fasterxml.jackson.databind.JsonNode text = s.get("Text");
          if (text != null)
            sb.append(text.asText()).append('\n');
        }
      } else if (root.has("text")) {
        sb.append(root.get("text").asText());
      } else {
        sb.append(json);
      }
      return sb.toString();
    } catch (Exception e) {
      return json;
    }
  }

  @GetMapping("/list")
  public List<Note> list(HttpSession session) {
    Long userId = requireUserId(session);
    if (userId == null)
      return java.util.Collections.emptyList();

    List<Note> notes = noteMapper.findByUserId(userId);

    // 自动检查未完成的任务状态
    for (Note n : notes) {
      if (n.getText() != null && n.getText().contains("【转写进行中】")) {
        NoteTask t = noteTaskMapper.findByNoteId(n.getId());
        if (t != null && t.getTaskId() != null) {
          try {
            FileTransServiceAliyun.Result r = fileTransService.query(t.getTaskId());
            if ("SUCCESS".equals(r.status)) {
              String realText = parsePlainText(r.resultText);
              if (realText != null) {
                if (n.getText() == null || !n.getText().equals(realText)) {
                  saveVersion(n);
                }
                noteMapper.updateText(n.getId(), realText);
                noteTaskMapper.updateStatus(t.getTaskId(), "SUCCESS");
                n.setText(realText); // 更新内存中的对象，以便直接返回最新结果
              }
            } else if ("FAILED".equals(r.status)) {
              String failMsg = "【转写失败】请重试";
              if (n.getText() == null || !n.getText().equals(failMsg)) {
                saveVersion(n);
              }
              noteMapper.updateText(n.getId(), failMsg);
              noteTaskMapper.updateStatus(t.getTaskId(), "FAILED");
              n.setText(failMsg);
            }
          } catch (Exception e) {
            // 忽略查询错误，避免阻塞列表加载
            e.printStackTrace();
          }
        }
      }
    }

    return notes;
  }

  @PostMapping("/update")
  public Object update(@RequestParam Long id, @RequestParam String text, HttpSession session) {
    Long userId = requireUserId(session);
    if (userId == null)
      return java.util.Collections.singletonMap("error", "未登录");
    Note note = noteMapper.findById(id);
    if (!canAccessNote(userId, note))
      return java.util.Collections.singletonMap("error", "无权限");
    saveVersion(note);
    noteMapper.updateText(id, text);
    return java.util.Collections.singletonMap("ok", true);
  }

  @PostMapping("/delete")
  public Object delete(@RequestParam Long id, HttpSession session) {
    Long userId = requireUserId(session);
    if (userId == null)
      return java.util.Collections.singletonMap("error", "未登录");
    Note note = noteMapper.findById(id);
    if (!canAccessNote(userId, note))
      return java.util.Collections.singletonMap("error", "无权限");
    saveVersion(note);
    noteMapper.delete(id);
    return java.util.Collections.singletonMap("ok", true);
  }

  @GetMapping("/search")
  public Object search(@RequestParam String q, HttpSession session) {
    Long userId = requireUserId(session);
    if (userId == null)
      return java.util.Collections.emptyList();
    return noteMapper.search(userId, q);
  }

  @PostMapping("/summarize")
  public Object summarize(@RequestParam Long id, HttpSession session) {
    try {
      Long userId = requireUserId(session);
      if (userId == null)
        return java.util.Collections.singletonMap("error", "未登录");
      Note note = noteMapper.findById(id);
      if (note == null) {
        return java.util.Collections.singletonMap("error", "笔记不存在");
      }
      if (!canAccessNote(userId, note))
        return java.util.Collections.singletonMap("error", "无权限");
      if (note.getText() == null || note.getText().trim().isEmpty()) {
        return java.util.Collections.singletonMap("error", "笔记内容为空，无法生成总结");
      }

      String summary = aiSummaryService.summarize(note.getText());
      saveVersion(note);
      noteMapper.updateSummary(id, summary);

      return java.util.Collections.singletonMap("summary", summary);
    } catch (Exception e) {
      return java.util.Collections.singletonMap("error", "总结生成失败: " + e.getMessage());
    }
  }

  @GetMapping("/versions")
  public Object versions(@RequestParam Long noteId, HttpSession session) {
    Long userId = requireUserId(session);
    if (userId == null)
      return java.util.Collections.singletonMap("error", "未登录");
    Note note = noteMapper.findById(noteId);
    if (!canAccessNote(userId, note))
      return java.util.Collections.singletonMap("error", "无权限");
    try {
      return noteVersionMapper.findMetaByNoteId(noteId);
    } catch (Exception e) {
      return java.util.Collections.singletonMap("error", "加载历史失败，请先执行数据库升级脚本");
    }
  }

  @GetMapping("/version")
  public Object version(@RequestParam Long id, HttpSession session) {
    Long userId = requireUserId(session);
    if (userId == null)
      return java.util.Collections.singletonMap("error", "未登录");
    NoteVersion v;
    try {
      v = noteVersionMapper.findById(id);
    } catch (Exception e) {
      return java.util.Collections.singletonMap("error", "加载版本失败，请先执行数据库升级脚本");
    }
    if (v == null)
      return java.util.Collections.singletonMap("error", "版本不存在");
    Note note = noteMapper.findById(v.getNoteId());
    if (!canAccessNote(userId, note))
      return java.util.Collections.singletonMap("error", "无权限");
    return v;
  }

  @PostMapping("/restore")
  public Object restore(@RequestParam Long versionId, HttpSession session) {
    Long userId = requireUserId(session);
    if (userId == null)
      return java.util.Collections.singletonMap("error", "未登录");
    NoteVersion v;
    try {
      v = noteVersionMapper.findById(versionId);
    } catch (Exception e) {
      return java.util.Collections.singletonMap("error", "恢复失败，请先执行数据库升级脚本");
    }
    if (v == null)
      return java.util.Collections.singletonMap("error", "版本不存在");
    Note note = noteMapper.findById(v.getNoteId());
    if (!canAccessNote(userId, note))
      return java.util.Collections.singletonMap("error", "无权限");
    saveVersion(note);
    noteMapper.updateAll(note.getId(), v.getTitle(), v.getText(), v.getSummary(), note.getAudioPath());
    return java.util.Collections.singletonMap("ok", true);
  }

  @GetMapping("/export/pdf")
  public void exportPdf(@RequestParam Long id, javax.servlet.http.HttpServletResponse response)
      throws java.io.IOException {
    try {
      Note note = noteMapper.findById(id);
      if (note == null) {
        response.sendError(404, "Note not found");
        return;
      }
      response.setContentType("application/pdf");
      String filename = java.net.URLEncoder.encode(note.getTitle(), "UTF-8").replaceAll("\\+", "%20");
      response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + ".pdf\"");
      exportService.exportToPdf(note, response.getOutputStream());
    } catch (Exception e) {
      e.printStackTrace();
      response.sendError(500, "Export failed: " + e.getMessage());
    }
  }

  @GetMapping("/export/word")
  public void exportWord(@RequestParam Long id, javax.servlet.http.HttpServletResponse response)
      throws java.io.IOException {
    try {
      Note note = noteMapper.findById(id);
      if (note == null) {
        response.sendError(404, "Note not found");
        return;
      }
      response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
      String filename = java.net.URLEncoder.encode(note.getTitle(), "UTF-8").replaceAll("\\+", "%20");
      response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + ".docx\"");
      exportService.exportToWord(note, response.getOutputStream());
    } catch (Exception e) {
      e.printStackTrace();
      response.sendError(500, "Export failed: " + e.getMessage());
    }
  }
}
