package com.study.kgraph.service;

import java.io.File;

public interface SpeechService {
    String transcribe(File audioFile) throws Exception;
}

