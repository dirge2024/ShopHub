package com.shophub.service.impl;

import com.shophub.entity.Follow;
import com.shophub.mapper.FollowMapper;
import com.shophub.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

}

