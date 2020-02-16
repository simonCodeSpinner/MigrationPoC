package com.accela.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class B2CUser {
	private String email;
	private String role;
	private String loginName;
	private String id;
	private String receiveSMS;
	private String active;
	private String registerDate;

	public B2CUser(String loginName, String email) {
		this.loginName = loginName;
		this.email = email;
	}

}
