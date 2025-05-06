package com.linkedout.auth.repository;

import com.linkedout.common.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

	/**
	 * 이메일로 계정 조회
	 *
	 * @param email 조회할 계정 이메일
	 * @return 해당 이메일의 계정 (Optional)
	 */
	Optional<Account> findByEmail(String email);
}
