package com.eatmate.post.service;

import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.post.PostRepository;
import com.eatmate.domain.entity.post.Post;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.domain.entity.user.Team;
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
    private final AccountRepository accountRepository;

    /**
     * 게시글 생성
     */

     public void createChatRoomAndTeamWhenWriteThePost(PostForm form){

        Account account = accountRepository.findByEmail(form.getAuthor());

         if (account == null){
             throw new IllegalArgumentException("해당 이메일을 가진 계정이 없습니다.");
         }

         //팀 만들기
        Team team = teamJpaService.createTeamWhenUserCreatePostAndChatRoom(form.getTeamName(), account);

        //게시글 생성
        Post post = Post.builder()
                .title(form.getTitle())
                .content(form.getDescription())
                .account(account)
                .team(team)
                .build();

         Post savedPost = postRepository.save(post);

        //TODO 나중에 채팅방으로 바꿀 생각

     }



}

//createTeam 말고 ChatRoom에서 account를 조회해서 가져와야 함