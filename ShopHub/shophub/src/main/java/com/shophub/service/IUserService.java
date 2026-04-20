package com.shophub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shophub.dto.LoginFormDTO;
import com.shophub.dto.Result;
import com.shophub.entity.User;

import javax.servlet.http.HttpSession;

public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);
}

