// WebSocket and STOMP initialization
var sock = new SockJS("/ws-stomp");
var ws = Stomp.over(sock);
var reconnect = 0;

// Vue.js instance
var vm = new Vue({
    el: '#app',

    data: {
        roomId: '',
        roomName: '',
        message: '',
        messages: [],
        token: '',
        userCount: 0
    },
    created() {
        try {
            // roomId, roomName 등을 서버에서 부터 불러온다
            this.roomId = localStorage.getItem('wschat.roomId') || 'defaultRoomId';
            this.roomName = localStorage.getItem('wschat.teamName') || '기본 이름';
            var _this = this;

            // 접속중인 계정의 JWT 토큰을 호출해서 사용자 정보를 가져오고,
            // JWT 토큰을 발급받아서 WebSocket 연결에 사용한다.
            axios.get('/user/info',{
            headers: {
                    'Accept': 'application/json'  // 서버가 JSON으로 응답하도록 요청
                }
            }).then(response => {
                // 서버에서 가져온 유저 데이터
                _this.token = response.data.token;

                // 서버로 연결을 시도하고,
                ws.connect({"token": _this.token}, function(frame) {
                    // 연결이 완료되면 ws.subscribe()로 해당 채팅방을 구독한다.
                    // "/sub/chat/room/" + _this.roomId 경로로 들어오는 메시지를 실시간으로 받아서 처리
                    ws.subscribe("/sub/chat/room/" + _this.roomId, function(message) {
                        console.log("구독자 : ", message);

                        var recv = JSON.parse(message.body);

                        _this.recvMessage(recv);

                    });
                }, function(error) {
                    alert("서버 연결에 실패 했습니다. 다시 접속해 주세요.");
                    location.href = "/chat/room";
                });
            });
        } catch (error) {
            console.error("에러 발생:", error);
        }
    },
    mounted() {
        // 페이지가 로드될 때 마지막 메시지로 스크롤
        this.scrollToBottom();
    },
    methods: {
        // 사용자가 메시지를 입력하고 '보내기' 버튼을 클릭하면
        // sendMessage('TALK') 메서드가 실행된다.
        // 사용자가 입력한 메시지와 메시지 타입을 WebSocket을 통해 서버로 전송
        sendMessage: function(type) {
            console.log("전송하는 type 값:", type);
            try {
                ws.send("/pub/chat/message", {"token": this.token}, JSON.stringify({
                    type: type,
                    roomId: this.roomId,
                    message: this.message
                }));
                this.message = '';
                // 메시지를 보낸 후에도 스크롤을 맨 아래로 이동
                this.$nextTick(() => {
                    this.scrollToBottom();
                });
            } catch (error) {
                console.error("메시지 전송 중 에러 발생:", error);
            }
        },

        // 새로 수신된 메시지를 화면에 추가한다.
        recvMessage: function(recv) {
            console.log("수신된 메시지:", recv);  // 수신된 메시지 출력
            this.$set(this.messages, this.messages.length, {
                "type": recv.type,
                "sender": recv.sender || "알 수 없음",
                "message": recv.message
            });

            // 새로운 메시지를 받은 후 스크롤을 맨 아래로 이동
            this.$nextTick(() => {
                this.scrollToBottom();
            });
        },

        // 채팅 리스트 맨 아래로 스크롤하는 함수
        scrollToBottom() {
            const container = this.$refs.messageContainer;
            container.scrollTop = container.scrollHeight;
        },

        exitRoom() {
            location.href = "/profile/list";
        }
    }
});
