/**
 * Copyright © 2016-2018 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.model.sql.CustomerEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID_STR;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
@SqlDao
public class JpaCustomerDao extends JpaAbstractSearchTextDao<CustomerEntity, Customer> implements CustomerDao {

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    protected Class<CustomerEntity> getEntityClass() {
        return CustomerEntity.class;
    }

    @Override
    protected CrudRepository<CustomerEntity, String> getCrudRepository() {
        return customerRepository;
    }

    @Override
    public List<Customer> findCustomers(TextPageLink pageLink) {
        return DaoUtil.convertDataList(customerRepository.findBy(
                Objects.toString(pageLink.getTextSearch(), ""),
                pageLink.getIdOffset() == null ? NULL_UUID_STR : UUIDConverter.fromTimeUUID(pageLink.getIdOffset()),
                new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<Customer> findCustomersByTenantId(UUID tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(customerRepository.findByTenantId(
                UUIDConverter.fromTimeUUID(tenantId),
                Objects.toString(pageLink.getTextSearch(), ""),
                pageLink.getIdOffset() == null ? NULL_UUID_STR : UUIDConverter.fromTimeUUID(pageLink.getIdOffset()),
                new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public Optional<Customer> findCustomersByTenantIdAndTitle(UUID tenantId, String title) {
        Customer customer = DaoUtil.getData(customerRepository.findByTenantIdAndTitle(UUIDConverter.fromTimeUUID(tenantId), title));
        return Optional.ofNullable(customer);
    }

    @Override
    public List<Customer> findCustomers() {
        return DaoUtil.convertDataList(customerRepository.findBy());
    }

    @Override
    public List<Customer> findCustomersByTenantId(UUID tenantId) {
        return DaoUtil.convertDataList(customerRepository.findByTenantId(UUIDConverter.fromTimeUUID(tenantId)));
    }

    @Override
    public int countCustomersByTenantId(UUID tenantId) {
        return customerRepository.countByTenantId(UUIDConverter.fromTimeUUID(tenantId));
    }

    @Override
    public Customer findCustomerByTitle(TenantId tenantId, String title) {
        return DaoUtil.getData(customerRepository.findByTitle(title));
    }
}
