package org.powertac.rachma.exec;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PersistentTaskRepository extends CrudRepository<PersistentTask, String> {

    Optional<PersistentTask> findFirstByStartIsNullOrderByPriorityAsc();
    Optional<PersistentTask> findFirstByStartIsNullOrderByPriorityDesc();

    @Modifying
    @Query("update PersistentTask t set t.priority = t.priority + :distance where t.priority < :exclusiveUpperBoundary")
    void shiftPrioritiesDown(int exclusiveUpperBoundary, int distance);

    @Modifying
    @Query("update PersistentTask t set t.priority = t.priority + :distance where t.priority > :exclusiveLowerBoundary")
    void shiftPrioritiesUp(int exclusiveLowerBoundary, int distance);

    @Query("select max(t.priority) from PersistentTask t where t.priority < :exclusiveUpperBoundary")
    Integer getNextLowerPriority(int exclusiveUpperBoundary);

    @Query("select min(t.priority) from PersistentTask t where t.priority > :exclusiveLowerBoundary")
    Integer getNextHigherPriority(@Param("exclusiveLowerBoundary") int exclusiveLowerBoundary);

}
