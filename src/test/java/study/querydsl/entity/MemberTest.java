package study.querydsl.entity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
//@Commit
class MemberTest {

	@PersistenceContext
	EntityManager em;

	@Test
	public void testEntity() {
		Team teamA = new Team("teamA");
		Team teamB = new Team("teamB");
		em.persist(teamA);
		em.persist(teamB);

		Member m1 = new Member("m1", 10, teamA);
		Member m2 = new Member("m1", 20, teamA);
		Member m3 = new Member("m1", 30, teamB);
		Member m4 = new Member("m1", 40, teamB);
		em.persist(m1);
		em.persist(m2);
		em.persist(m3);
		em.persist(m4);

		// 초기화
		em.flush();
		em.clear(); // 영속성 컨텍스트 clear, clear cache

		List<Member> members = em.createQuery("select m from Member m", Member.class)
			.getResultList();

		for (Member member:members) {
			System.out.println("member = " + member);
			System.out.println(" -> member.team = " + member.getTeam());

		}

	}

}
