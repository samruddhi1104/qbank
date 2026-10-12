package com.qbank.qbank;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("SELECT q FROM Question q WHERE "
         + "(:keyword = '' OR LOWER(q.text) LIKE LOWER(CONCAT('%', :keyword, '%')) "
         + "OR LOWER(q.subject) LIKE LOWER(CONCAT('%', :keyword, '%')) "
         + "OR LOWER(q.topic) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
         + "AND (:status = '' OR q.status = :status) ORDER BY q.id")
    List<Question> search(@Param("keyword") String keyword, @Param("status") String status);

    long countByStatus(String status);
}