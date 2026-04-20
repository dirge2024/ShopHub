package com.shophub.service.impl;

import com.shophub.entity.BlogComments;
import com.shophub.mapper.BlogCommentsMapper;
import com.shophub.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}

