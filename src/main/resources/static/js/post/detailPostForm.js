function joinTeam(teamId) {
    // 팀 가입을 위한 POST 요청

    axios.post('/team/join/' + teamId)

        .then(response => {

            // 서버로부터 roomId, userNickname, teamName 데이터를 받아옴
            const roomId = response.data.roomId;
            const userNickname = response.data.userNickname;
            const teamName = response.data.teamName;

            // userNickname과 teamName을 localStorage에 저장
            localStorage.setItem('wschat.userNickname', userNickname);
            localStorage.setItem('wschat.teamName', teamName);

            if (roomId) {
                // 채팅방 입장 화면으로 리다이렉트
                window.location.href = "/chat/room/enter/" + roomId;

            } else {
                alert('채팅방 정보를 불러오지 못했습니다.');
            }
        })
        .catch(error => {
            console.error("팀 가입 중 오류가 발생했습니다.", error);
            alert('팀 가입에 실패했습니다.');
        });

}
