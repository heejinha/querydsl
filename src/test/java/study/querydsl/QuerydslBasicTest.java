package study.querydsl;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
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

	/**
	 * 1. 회원 나이 desc
	 * 2. 회원 이름 asc
	 * 단, 2에서 회원 이름이 없으면 마지막에 출력
	 */
	@Test
	public void sort() {
		em.persist(new Member("m5", 100));
		em.persist(new Member("m6", 100));
		em.persist(new Member(null, 100));

		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.eq(100))
			.orderBy(member.age.desc(), member.username.asc().nullsLast())
			.fetch();

		Member m5 = result.get(0);
		Member m6 = result.get(1);
		Member memberNull = result.get(2);

		assertThat(m5.getUsername()).isEqualTo("m5");
		assertThat(m6.getUsername()).isEqualTo("m6");
		assertThat(memberNull.getUsername()).isNull();
	}

	@Test
	public void paging1() {
		List<Member> result = queryFactory
			.selectFrom(member)
			.orderBy(member.username.desc())
			.offset(1)
			.limit(2)
			.fetch();

		assertThat(result.size()).isEqualTo(2);
	}

	@Test
	public void aggregation() {
		List<Tuple> result = queryFactory
			.select(
				member.count(),
				member.age.sum(),
				member.age.avg(),
				member.age.max(),
				member.age.max()
			)
			.from(member)
			.fetch();

		Tuple tuple = result.get(0);
		assertThat(tuple.get(member.count())).isEqualTo(4);
		assertThat(tuple.get(member.age.sum())).isEqualTo(100);
	}

	/**
	 * team의 이름과 각 팀의 평균 연령을 구하라
	 */

	@Test
	public void group() {
		List<Tuple> result = queryFactory
			.select (team.name, member.age.avg())
			.from(member)
			.join(member.team, team)
			.groupBy(team.name)
			.fetch();

		Tuple teamA = result.get(0);
		Tuple teamB = result.get(1);

		assertThat(teamA.get(team.name)).isEqualTo("teamA");
		assertThat(teamA.get(member.age.avg())).isEqualTo(15);

		assertThat(teamB.get(team.name)).isEqualTo("teamB");
		assertThat(teamB.get(member.age.avg())).isEqualTo(35);                        
	}

	/**
	 * teamA 에 소속된 모든 회원을 조회
	 */
	@Test
	public void join() {
		List<Member> result = queryFactory
			.selectFrom(member)
			.join(member.team, team)
			.where(team.name.eq("teamA"))
			.fetch();

		assertThat(result)
			.extracting("username")
			.containsExactly("m1", "m2");   
	}

	/**
	 * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
	 * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
	 * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and
		t.name='teamA'
	*/
	@Test
	public void join_on_filtering() {
		List<Tuple> result = queryFactory
			.select(member, team)
			.from(member)
			.leftJoin(member.team, team).on(team.name.eq("teamA"))
			.fetch();

		for (Tuple tuple : result) {
			System.out.println("==== tuple : " + tuple);
			
		}
	} 

	/**
	 * 2. 연관관계 없는 엔티티 외부 조인
	 * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
	 * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
	 * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
	 */
	@Test
	public void join_on_no_relation() throws Exception {
		em.persist(new Member("teamA"));   
		em.persist(new Member("teamB"));
		em.persist(new Member("teamC"));

		List<Tuple> result = queryFactory
			.select(member, team)
			.from(member)
			.leftJoin(team).on(member.username.eq(team.name))
			.fetch();

		for (Tuple tuple : result) {
			System.out.println("==== tuple = " + tuple);
		}
	}

	@PersistenceUnit
	EntityManagerFactory emf;

	@Test
	public void fetchJoinNo() {
		em.flush();
		em.clear();

		Member fetchMember = queryFactory
			.selectFrom(member)
			.where(member.username.eq("m1"))
			.fetchOne();

		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(fetchMember.getTeam());
		assertThat(loaded).isFalse();
	}

	@Test
	public void fetchJoin() {
		em.flush();
		em.clear();

		Member fetchMember = queryFactory
			.selectFrom(member)
			.join(member.team, team).fetchJoin()
			.where(member.username.eq("m1"))
			.fetchOne();

		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(fetchMember.getTeam());
		assertThat(loaded).isTrue();
	}

	@Test
	public void subQuery() {
		QMember memberSub = new QMember("memberSub");
		List<Member> result = queryFactory
								.selectFrom(member)
								.where(member.age.eq(
											select(memberSub.age.max())
											.from(memberSub)
										))
								.fetch();
		assertThat(result).extracting("age")
			.containsExactly(40);
	}

	@Test
	public void simpleProjection() {
		List<String> result = queryFactory
			.select(member.username)
			.from(member)
			.fetch();
		System.out.println(result);
	}
        
	@Test
	public void tupleProjection() {
		List<Tuple> result = queryFactory
			.select(member.username, member.age)
			.from(member)
			.fetch();
		System.out.println(result);

		for (Tuple tuple : result) {
			String username = tuple.get(member.username);
			Integer age = tuple.get(member.age);

			System.out.println(username);
			System.out.println(age);
		}
	}

	@Test
	public void findDtoByJPQL() {
		List<MemberDto> result = em.createQuery("select ne          w study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
			.getResultList();
		for (MemberDto memberDto : result) {
			System.out.println("memberDto = " + memberDto);
		}
	}

	@Test
	public void findDtoBySetter() {
		List<MemberDto> result = queryFactory
			.select(Projections.fields(MemberDto.class, member.username, member.age))
			.from(member)
			.fetch();
		
		for (MemberDto memberDto : result) {
  			System.out.println("memberDto = " + memberDto);
		}
	}

	@Test
	public void findUserDtoBySetter() {
		List<UserDto> result = queryFactory
			.select(Projections.fields(UserDto.class,
							member.username.as("name"), 
							member.age ))
			.from(member)
			.fetch();
		
		for (UserDto userDto : result) {    
  			System.out.println("userDto = " + userDto);
		}
	}

	@Test
	public void findByUserDtoConstructor(){

		List<UserDto> result = queryFactory
			.select(Projections.constructor(UserDto.class,
				member.username,
				member.age)
			)
			.from(member)
			.fetch();
		
		for (UserDto userDto : result) {
  			System.out.println("userDto = " + userDto);
		}

	}

	@Test
	public void findByQueryProjection() {
		List<MemberDto> result = queryFactory
			.select(new QMemberDto(member.username, member.age))
			.from(member)
			.fetch();

		for (MemberDto memberDto : result) {
			System.out.println("memberDto => " + memberDto);
			
		}
	}

	@Test
	public void dynamicQuery_BooleanBuilder() {
		String usernameParam = "m1";
		Integer ageParam = null;
		
		List<Member> result = searchMember1(usernameParam,ageParam);
		assertThat(result.size()).isEqualTo(1);
	}

	private List<Member> searchMember1(String usernameParam, Integer ageParam) {

		BooleanBuilder builder = new BooleanBuilder();
		if (usernameParam != null) {
			builder.and(member.username.eq(usernameParam));
		}

		if (ageParam != null) {
			builder.and(member.age.eq(ageParam));
		}

		return queryFactory
			.selectFrom(member)
			.where(builder)
			.fetch();
	}


	@Test
	public void dynamicQuery_Predicate() {
		String usernameParam = "m1";
		Integer ageParam = null;
		
		List<Member> result = searchMember2(usernameParam,ageParam);
		assertThat(result.size()).isEqualTo(1);
	}

	private List<Member> searchMember2(String usernameCond, Integer ageCond) {
		return queryFactory
					.selectFrom(member)
					.where(usernameEq(usernameCond),ageEg(ageCond))
					.fetch();
	}

	private BooleanExpression usernameEq(String usernameCond) {
		return (usernameCond == null)
					? null : member.username.eq(usernameCond);
	}

	private BooleanExpression ageEg(Integer ageCond) {
		return (ageCond == null)
					? null : member.age.eq(ageCond);                     
	}

	@Test
	public void bulkUpdate() {
		long count = queryFactory
			.update(member)
			.set(member.username, "비회원")
			.where(member.age.lt(28))
			.execute();
		System.out.println("===================================");
		System.out.println("======>" + count);

		em.flush();
		em.clear();

		List<Member> result = queryFactory.selectFrom(member).fetch();
		for (Member member2 : result) {
			System.out.println("======>" + member2);
		}
	}

}
