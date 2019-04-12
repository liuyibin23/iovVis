package org.thingsboard.server.dao.sql.task;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.common.data.task.TaskKind;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.AlarmEntity;
import org.thingsboard.server.dao.model.sql.RelationEntity;
import org.thingsboard.server.dao.model.sql.TaskEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTimeDao;
import org.thingsboard.server.dao.task.TaskDao;
import org.thingsboard.server.dao.util.SqlDao;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.data.jpa.domain.Specifications.where;

@Slf4j
@Component
@SqlDao
public class JpaTaskDao extends JpaAbstractDao<TaskEntity, Task> implements TaskDao {

    @Autowired
    private TaskRepository taskRepository;

    @Override
    protected Class<TaskEntity> getEntityClass() {
        return TaskEntity.class;
    }

    @Override
    protected CrudRepository<TaskEntity, String> getCrudRepository() {
        return taskRepository;
    }

    @Override
    public ListenableFuture<Task> findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, TaskKind taskType) {
        return service.submit(() -> {
            List<TaskEntity> latest = taskRepository.findLatestByOriginatorAndType(
                    UUIDConverter.fromTimeUUID(tenantId.getId()),
                    UUIDConverter.fromTimeUUID(originator.getId()),
                    originator.getEntityType(),
                    taskType,
                    new PageRequest(0, 1));
            return latest.isEmpty() ? null : DaoUtil.getData(latest.get(0));
        });
    }

    @Override
    public List<Task> checkTasks() {

        return DaoUtil.convertDataList(taskRepository.findAll());
    }

    @Override
    public List<Task> checkTasks(TenantId tenantId) {
        return DaoUtil.convertDataList(taskRepository.findAllByTenantId(UUIDConverter.fromTimeUUID(tenantId.getId())));
    }

    @Override
    public List<Task> checkTasks(TenantId tenantId, CustomerId customerId) {
        return DaoUtil.convertDataList(
                taskRepository.findAllByTenantIdAndCustomerId(UUIDConverter.fromTimeUUID(tenantId.getId()),
                        UUIDConverter.fromTimeUUID(customerId.getId()))
        );
    }

    @Override
    public List<Task> findTasksByUserId(UserId userId) {
        return DaoUtil.convertDataList(
                taskRepository.findAllByUserId(UUIDConverter.fromTimeUUID(userId.getId()))
        );
    }

    @Override
    public Task findTaskById(UUID id) {
        return taskRepository.findOne(UUIDConverter.fromTimeUUID(id)).toData();
    }

    @Override
    public ListenableFuture<List<Task>> findTasks(TenantId tenantId, CustomerId customerId, TimePageLink pageLink) {
        Specification<TaskEntity> timeSearchSpec = JpaAbstractSearchTimeDao.getTimeSearchPageSpec(pageLink, "id");
        Specification<TaskEntity> fieldsSpec = getEntityFieldsSpec(tenantId, customerId, pageLink);
        Sort.Direction sortDirection = pageLink.isAscOrder() ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = new PageRequest(0, pageLink.getLimit(), sortDirection, "id");
        return service.submit(() ->
                DaoUtil.convertDataList(taskRepository.findAll(where(timeSearchSpec).and(fieldsSpec), pageable).getContent()));
    }

    private Specification<TaskEntity> getEntityFieldsSpec(TenantId tenantId, CustomerId customerId, TimePageLink pageLink) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (tenantId != null) {
                Predicate tenantIdPredicate = criteriaBuilder.equal(root.get("tenantId"),  UUIDConverter.fromTimeUUID(tenantId.getId()));
                predicates.add(tenantIdPredicate);
            }
            if (customerId != null) {
                Predicate customIdPredicate = criteriaBuilder.equal(root.get("customerId"),  UUIDConverter.fromTimeUUID(customerId.getId()));
                predicates.add(customIdPredicate);
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
