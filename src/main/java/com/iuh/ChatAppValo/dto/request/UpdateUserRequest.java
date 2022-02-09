package com.iuh.ChatAppValo.dto.request;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;

import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	
	private String name;
	
	private String gender;

    private Date dateOfBirth;

    private String email;

    private String address;

    private String imgUrl;

}
