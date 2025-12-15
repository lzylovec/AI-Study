package com.study.kgraph.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
  @GetMapping("/")
  public String index() {
    return "login";
  }

  @GetMapping("/dashboard")
  public String dashboard() {
    return "dashboard";
  }

  @GetMapping("/notes")
  public String notes() {
    return "notes";
  }

  @GetMapping("/graph")
  public String graph() {
    return "graph";
  }

  @GetMapping("/upload-audio")
  public String uploadAudio() {
    return "upload_audio";
  }

  @GetMapping("/upload-long-audio")
  public String uploadLongAudio() {
    return "upload_long_audio";
  }

  @GetMapping("/upload-material")
  public String uploadMaterial() {
    return "upload_material";
  }

  @GetMapping("/upload-doc")
  public String uploadDoc() {
    return "upload_doc";
  }

  @GetMapping("/upload-audio-center")
  public String uploadAudioCenter() {
    return "upload_audio_center";
  }

  @GetMapping("/upload-center")
  public String uploadCenter() {
    return "upload_center";
  }

  @GetMapping("/register")
  public String register() {
    return "register";
  }
}
