package com.plumsnow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plumsnow.entity.Blog;
import com.plumsnow.mapper.BlogMapper;
import com.plumsnow.service.BlogService;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author plumsnow
 * @since 2024
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements BlogService {

}
