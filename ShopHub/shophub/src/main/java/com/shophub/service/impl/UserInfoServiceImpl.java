package com.shophub.service.impl;

import com.shophub.entity.UserInfo;
import com.shophub.mapper.UserInfoMapper;
import com.shophub.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}

