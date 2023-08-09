package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

	@PersistenceContext
	EntityManager em;

	JPAQueryFactory queryFactory;

	@BeforeEach
	public void before() {
		queryFactory = new JPAQueryFactory(em);

		Team teamA = new Team("teamA");
		Team teamB = new Team("teamB");
		em.persist(teamA);
		em.persist(teamB);

		Member m1 = new Member("m1", 10, teamA);
		Member m2 = new Member("m2", 20, teamA);
		Member m3 = new Member("m3", 30, teamB);
		Member m4 = new Member("m4", 40, teamB);
		em.persist(m1);
		em.persist(m2);
		em.persist(m3);
		em.persist(m4);
	}

	@Test
	public void startJPQL() {
		// find member1
		Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
			.setParameter("username", "m1")
			.getSingleResult();

		assertThat(findMember.getUsername()).isEqualTo("m1");
	}

	@Test
	public void startQuerydsl() {
		Member findMember = queryFactory
			.select(member)
			.from(member)
			.where(member.username.eq("m1"))
			.fetchOne();
		assertThat(findMember.getUsername()).isEqualTo("m1");
	}

	@Test
	public void search() {
		Member findMember = queryFactory
			.selectFrom(member)
			.where(
				member.username.eq("m1")
					.and(member.age.eq(10))
			)
			.fetchOne();

		assertThat(findMember.getAge()).isEqualTo(10);
	}

	@Test
	public void searchAndParam() {
		Member findMember = queryFactory
			.selectFrom(member)
			.where(
				member.username.eq("m1"),
				member.age.eq(10)
			)
			.fetchOne();

		assertThat(findMember.getAge()).isEqualTo(10);
	}

	@Test
	public void resultFetch() {
		List<Member> fetch = queryFactory
			.selectFrom(member)
			.fetch();

		Member fetchOne = queryFactory
			.selectFrom(member)
			.fetchOne();

		Member fetchFirst = queryFactory
			.selectFrom(member)
			.fetchFirst();

	}





}
