package com.mztalk.mentor.repository.impl;

import com.mztalk.mentor.domain.entity.Score;
import com.mztalk.mentor.repository.ScoreRepositoryCustom;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import java.util.List;

public class ScoreRepositoryCustomImpl implements ScoreRepositoryCustom {

    @Autowired
    private EntityManager entityManager;

    public Double findScoreByBoardId(Long id){
        Double avgScore = entityManager.createQuery("select avg(s.count) from Board b join b.mentor m join m.scores s where b.id=:id", Double.class)
                .setParameter("id", id)
                .getSingleResult();
        return avgScore;
    }

    @Override
    public List<Score> findByNickname(String nickname) {
        List<Score> scores = entityManager.createQuery("select s from Score s join s.mentor m join m.board b where b.nickname =:nickname", Score.class)
                .setParameter("nickname", nickname)
                .getResultList();
        return scores;
    }
}
