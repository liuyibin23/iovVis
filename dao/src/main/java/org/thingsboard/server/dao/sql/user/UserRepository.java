/**
 * Copyright © 2016-2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.user;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.model.sql.UserEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

/**
 * @author Valerii Sosliuk
 */
@SqlDao
public interface UserRepository extends CrudRepository<UserEntity, String> {

    UserEntity findByEmail(String email);

    int countByTenantIdAndAuthority(String tenantId, Authority authority);



    int countByTenantIdAndCustomerIdAndAuthority(String tenantId,String customerId,Authority authority);

    @Query("SELECT COUNT(u.additionalInfo) FROM UserEntity u WHERE u.tenantId = :tenantId AND u.authority = :authority " +
			"AND u.additionalInfo LIKE CONCAT('%power%',:likeStr,'%') ")
    int countByTenantIdAndAuthorityAndAdditionalInfoLike(@Param("tenantId")String tenantId,@Param("authority") Authority authority,@Param("likeStr")String likeStr);

//	int countByAdditionalInfoLike(String likeStr);

    @Query("SELECT u FROM UserEntity u WHERE (:tenantId is null OR u.tenantId = :tenantId) " +
            "AND (:customerId is null OR u.customerId = :customerId)  AND u.authority = :authority " +
            "AND LOWER(u.searchText) LIKE LOWER(CONCAT(:searchText, '%'))" +
            "AND u.id > :idOffset ORDER BY u.id")
    List<UserEntity> findUsersByAuthority(@Param("tenantId") String tenantId,
                                          @Param("customerId") String customerId,
                                          @Param("idOffset") String idOffset,
                                          @Param("searchText") String searchText,
                                          @Param("authority") Authority authority,
                                          Pageable pageable);

	@Query("SELECT u FROM UserEntity u WHERE u.customerId = :customerId AND u.authority = :authority " +
			"AND LOWER(u.searchText) LIKE LOWER(CONCAT(:searchText, '%'))" +
			"AND u.id > :idOffset ORDER BY u.id")
	List<UserEntity> findUsersByAuthority(@Param("customerId") String customerId,
										  @Param("idOffset") String idOffset,
										  @Param("searchText") String searchText,
										  @Param("authority") Authority authority,
										  Pageable pageable);


	@Query("SELECT u FROM UserEntity u WHERE" +
			" LOWER(u.searchText) LIKE LOWER(CONCAT('%',:searchText, '%'))" +
			"AND u.id > :idOffset ORDER BY u.id")
	List<UserEntity> findUsers( @Param("idOffset") String idOffset,
							  @Param("searchText") String searchText,
							  Pageable pageable);

	List<UserEntity> findAllByTenantIdAndAuthority(String tenantId,Authority authority);

	List<UserEntity> findAllByFirstNameLikeAndLastNameLike(String firstName,String lastName);

	List<UserEntity> findAllByFirstNameLike(String firstName);

	UserEntity findFirstByFirstName(String firstName);

	@Query("SELECT u FROM UserEntity u ")
	List<UserEntity> findAllUsers();

	@Query("SELECT u FROM UserEntity u WHERE " +
			"u.tenantId = :tenantId " +
			"AND u.customerId = :customerId " +
			"AND LOWER(u.searchText) LIKE LOWER(CONCAT('%',:searchText, '%'))" +
			"AND u.id > :idOffset ORDER BY u.id")
	List<UserEntity> findUsersByTenantAndCustomerId(@Param("tenantId") String tenantId,
													@Param("customerId") String customerId,
													@Param("idOffset") String idOffset,
													@Param("searchText") String searchText,
													Pageable pageable);

	@Query("SELECT u FROM UserEntity u WHERE " +
			"u.tenantId = :tenantId " +
			"AND LOWER(u.searchText) LIKE LOWER(CONCAT('%',:searchText, '%'))" +
			"AND u.id > :idOffset ORDER BY u.id")
	List<UserEntity> findUsersByTenantId(@Param("tenantId") String tenantId,
										 @Param("idOffset") String idOffset,
										 @Param("searchText") String searchText,
										 Pageable pageable);

	UserEntity findFirstByCustomerId(String customerId);
}