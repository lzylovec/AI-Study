package com.study.kgraph.controller;

import com.study.kgraph.entity.User;
import com.study.kgraph.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  @Autowired
  private UserMapper userMapper;

  @PostMapping("/register")
  public Object register(@RequestParam String username, @RequestParam String password) {
    User existing = userMapper.findByUsername(username);
    if (existing != null)
      return java.util.Collections.singletonMap("error", "用户名已存在");
    User u = new User();
    u.setUsername(username);
    u.setPasswordHash(DigestUtils.md5DigestAsHex(password.getBytes()));
    userMapper.insert(u);
    return java.util.Collections.singletonMap("ok", true);
  }

  @PostMapping("/login")
  public Object login(@RequestParam String username, @RequestParam String password, HttpSession session) {
    User u = userMapper.findByUsername(username);
    if (u == null)
      return java.util.Collections.singletonMap("error", "用户不存在");
    String hash = DigestUtils.md5DigestAsHex(password.getBytes());
    if (!hash.equals(u.getPasswordHash()))
      return java.util.Collections.singletonMap("error", "密码错误");
    session.setAttribute("userId", u.getId());
    return java.util.Collections.singletonMap("ok", true);
  }

  @PostMapping("/logout")
  public Object logout(HttpSession session) {
    session.invalidate();
    return java.util.Collections.singletonMap("ok", true);
  }
}
