package com.eatmate.post.service;

import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.post.PostRepository;
import com.eatmate.dao.repository.team.TeamRepository;
import com.eatmate.domain.entity.post.Post;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.Team;
import com.eatmate.global.service.FileStore.FileStoreOfPost;
import com.eatmate.post.vo.PostForm;
import com.eatmate.team.service.TeamJpaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostJpaService {

    private final PostRepository postRepository;
    private final TeamJpaService teamJpaService;
    /**
     * 게시글 생성
     */
     public void createChatRoomAndTeamWhenWriteThePost(PostForm form){
        /**
            게시글 생성 데이터가 form을 통해 들어온다.
            Team을 만들고,
            Team에 author의 id를 등록한다.
            author의 email로 accountRepository에서 엔티티를 호출해서, AccountTeam 객체를 만든다.
            만들어진 객체를 Team에 등록한다.
            이때, boolean 값은 true로 설정

         1) 그냥 Account에 등록해두면
         1)
         Team -> AccountTeam -> Account를 통해서
         대충 해당 게시글 불러올 수 있다.

         2)
         Team과 해당 Post의 관계를
         OneToOne으로 불러온다.
         값 불러오기 편해진다.
         */

        //팀 만들기
         Team team = teamJpaService.createTeamWhenUserCreatePost(form.getTeamName(), form.getAuthor());
        //게시글 생성
        // TODO : 게시글 -> 채팅방으로 바꿔야 함
        Post post = Post.builder()
             .team(team)
             .build();

         Post savedPost = postRepository.save(post);

        //TODO 나중에 채팅방으로 바꿀 생각

     }



}
