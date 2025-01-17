package com.plumsnow.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class BlogPageQueryDTO implements Serializable {

    //页码
    private int page;

    //每页记录数
    private int pageSize;

    //所属商店id
    private Long shopId;

}
