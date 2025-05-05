package com.comment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.comment.entity.BlogComments;
import com.comment.mapper.BlogCommentsMapper;
import com.comment.service.IBlogCommentsService;
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
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
