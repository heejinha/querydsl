package study.querydsl.repository;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import study.querydsl.entity.Member;

@SpringBootTest
@Transactional
public class MemberJPARepositoryTest {

    @Autowired EntityManager em;
    @Autowired MemberJPARepository memberJPARepository;

    @Test
    void basicTest() {
        Member member = new Member("member1", 10);
        memberJPARepository.save(member);

        Member findMember = memberJPARepository.findById(member.getId()).get();
        Assertions.assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberJPARepository.findAll_JPQL();
        Assertions.assertThat(result1).containsExactly(member);

        List<Member> result2 = memberJPARepository.findByUsername_JPQL(member.getUsername());
        Assertions.assertThat(result2).containsExactly(member);
    }

    @Test
    void basicQuerydslTest() {
        Member member = new Member("member1", 10);
        memberJPARepository.save(member);

        Member findMember = memberJPARepository.findById(member.getId()).get();
        Assertions.assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberJPARepository.findAll_Querydsl();
        Assertions.assertThat(result1).containsExactly(member);

        List<Member> result2 = memberJPARepository.findByUsername_Querydsl(member.getUsername());
        Assertions.assertThat(result2).containsExactly(member);
    }

    @Test
    void testFindAll() {

    }

    @Test
    void testFindById() {

    }

    @Test
    void testFindByUsername() {

    }

    @Test
    void testSave() {

    }
}
