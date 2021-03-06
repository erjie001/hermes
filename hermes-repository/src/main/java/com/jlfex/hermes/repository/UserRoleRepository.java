package com.jlfex.hermes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jlfex.hermes.model.UserRole;

/**
 * 用户角色仓库
 * 
 * @author ultrafrog
 * @version 1.0, 2013-12-10
 * @since 1.0
 */
public interface UserRoleRepository extends JpaRepository<UserRole, String> {

	/**
	 * 通过用户编号查询所有角色
	 * 
	 * @param userId
	 * @return
	 */
	public List<UserRole> findByUserId(String userId);
}
