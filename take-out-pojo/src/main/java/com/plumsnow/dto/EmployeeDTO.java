package com.plumsnow.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class EmployeeDTO implements Serializable {

    private Long id;

    private String username;

    private String name;

    private String phone;

    private String sex;

    private String idNumber;

    public EmployeeDTO(Long id, String username, String name, String phone, String sex, String idNumber){
        this.id = id;
        this.username = username;
        this.name = name;
        this.phone = phone;
        this.sex = sex;
        this.idNumber = idNumber;
    }

    public EmployeeDTO(){}
}
