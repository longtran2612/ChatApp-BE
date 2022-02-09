package com.iuh.ChatAppValo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;


@Document
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User{

    @Id
    private  String id;

    private String name;

    private String gender;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Ho_Chi_Minh")
    private Date dateOfBirth;

    @Indexed(background = true, direction = IndexDirection.ASCENDING)
    private String phone;

    @Indexed(background = true, direction = IndexDirection.ASCENDING)
    private String email;

    private String address;

    private String imgUrl;

    private String status;

}
