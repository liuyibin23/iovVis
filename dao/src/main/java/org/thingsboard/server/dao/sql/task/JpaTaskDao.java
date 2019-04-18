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
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.common.data.task.TaskKind;
import org.thingsboard.server.common.data.task.TaskQuery;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.TaskEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTimeDao;
import org.thingsboard.server.dao.task.TaskDao;
import org.thingsboard.server.dao.util.SqlDao;

import javax.persistence.criteria.CriteriaBuilder;
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
    public ListenableFuture<List<Task>> findTasks(TaskQuery query, TimePageLink pageLink) {
        Specification<TaskEntity> timeSearchSpec = JpaAbstractSearchTimeDao.getTimeSearchPageSpec(pageLink, "id");
        Specification<TaskEntity> fieldsSpec = getEntityFieldsSpec(query);
        Sort.Direction sortDirection = pageLink.isAscOrder() ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = new PageRequest(0, pageLink.getLimit(), sortDirection, "id");
        return service.submit(() ->
                DaoUtil.convertDataList(taskRepository.findAll(where(timeSearchSpec).and(fieldsSpec), pageable).getContent()));
    }

    private Specification<TaskEntity> getEntityFieldsSpec(TaskQuery query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.getTenantId() != null) {
                Predicate tenantIdPredicate = criteriaBuilder.equal(root.get("tenantId"), UUIDConverter.fromTimeUUID(query.getTenantId().getId()));
                predicates.add(tenantIdPredicate);
            }
            if (query.getCustomerId() != null) {
                Predicate customIdPredicate = criteriaBuilder.equal(root.get("customerId"), UUIDConverter.fromTimeUUID(query.getCustomerId().getId()));
                predicates.add(customIdPredicate);
            }
            if (query.getTaskKind() != null) {
                Predicate taskKindPredicate = criteriaBuilder.equal(root.get("taskKind"), query.getTaskKind());
                predicates.add(taskKindPredicate);
            }
            if (query.getUserIdList() != null && !query.getUserIdList().isEmpty()) {
                CriteriaBuilder.In<String> in =  criteriaBuilder.in(root.get("userId"));
                query.getUserIdList().forEach(userId-> in.value(UUIDConverter.fromTimeUUID(userId.getId())));
                predicates.add(in);
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
