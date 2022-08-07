package jpabook.jpashop.api;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class MemberApiController {
    private final MemberService memberService;

    /*
    * 응답 값으로 Eneity를 직접 외부에 노출.
    * 응답 스펙을 맞추기 위한 로직이 추가됨(@JsonIgnore)
    * 절대 Entity를 그대로 요청,응답 하지 말고 DTO를 따로 만들어 구현할 것.
    * */
    @GetMapping("/api/v1/members")
    public List<Member> membersV1(){
        return memberService.findMembers();
    }

    /*
    * Result라는 껍데기 객체로 감싸서 List를 반환.
    * Collection을 그대로 반환하게 되면 API스펙을 변환하기 어려움.
    * */
    @GetMapping("/api/v2/members")
    public Result memberV2(){
        List<Member> findMembers = memberService.findMembers();
        List<MemberDto> collect = findMembers.stream()
                .map(m -> new MemberDto(m.getName()))
                .collect(Collectors.toList());

        return new Result(collect);

    }
    @Data
    @AllArgsConstructor
    private static class Result<T> {
        private T data;
    }
    @Data
    @AllArgsConstructor
    static class MemberDto{
        private String name;

    }

    /*
    * API설계시 Entity를 Request로, Response로 하면 안됨(API 스펙 변경시 어려움 발생)
    * */
    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member){
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }
    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid CreateMemberRequest request){
        Member member = new Member();
        member.setName(request.getName());
        Long id = memberService.join(member);

        return new CreateMemberResponse(id);

    }
    @PutMapping("/api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(
            @PathVariable("id") Long id,
           @RequestBody @Valid UpdateMemberRequest request){
        memberService.update(id, request.getName());
        Member findMember = memberService.findOne(id);
        return new UpdateMemberResponse(findMember.getId(),findMember.getName());
    }
    @Data
    private static class CreateMemberRequest {
        private String name;

    }

    private class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }

    @Data
    @AllArgsConstructor
    private class UpdateMemberResponse {
        private Long id;
        private String name;
    }
    @Data
    private static class UpdateMemberRequest {
        private String name;

    }
}
