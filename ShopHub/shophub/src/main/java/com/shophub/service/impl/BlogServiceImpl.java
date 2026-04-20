package com.shophub.service.impl;

import com.shophub.entity.Blog;
import com.shophub.mapper.BlogMapper;
import com.shophub.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

}

