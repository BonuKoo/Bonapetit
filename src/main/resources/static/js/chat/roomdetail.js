// WebSocket and STOMP initialization
var sock = new SockJS("/ws-stomp");
var ws = Stomp.over(sock);
var reconnect = 0;

// 한 번에 불러올 내역 건수. 서버 상한은 100이다.
var HISTORY_PAGE_SIZE = 50;

// Vue.js instance
var vm = new Vue({
    el: '#app',

    data: {
        roomId: '',
        roomName: '',
        message: '',
        messages: [],
        token: '',
        userCount: 0,
        // 커서 페이징 상태
        nextCursor: null,
        hasMore: false,
        loadingHistory: false
    },
    created() {
        try {
            // roomId, roomName 등을 localStorage 에서 부터 불러온다
            this.roomId = localStorage.getItem('wschat.roomId') || 'defaultRoomId';
            this.roomName = localStorage.getItem('wschat.roomName') || '기본 이름';
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

                // 이전 대화 내역을 먼저 채운 뒤 실시간 구독을 시작한다.
                // 순서를 바꾸면 구독으로 들어온 메시지가 내역에 덮여 사라질 수 있다.
                _this.loadInitialHistory();

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
        // 채팅방 진입 시 최신 내역을 불러온다.
        loadInitialHistory: function() {
            var _this = this;
            this.loadingHistory = true;

            axios.get('/chat/room/' + this.roomId + '/messages', {
                params: { size: HISTORY_PAGE_SIZE }
            }).then(function(response) {
                var data = response.data;
                // 서버가 오래된 것 -> 최신 순으로 내려주므로 그대로 쓰면 된다.
                _this.messages = data.messages;
                _this.nextCursor = data.nextCursor;
                _this.hasMore = data.hasMore;

                _this.$nextTick(function() {
                    _this.scrollToBottom();
                });
            }).catch(function(error) {
                // 내역을 못 불러와도 실시간 대화는 되어야 하므로 알림만 남긴다.
                console.error("채팅 내역을 불러오지 못했습니다:", error);
            }).then(function() {
                _this.loadingHistory = false;
            });
        },

        // 위로 스크롤했을 때 더 과거 메시지를 이어 붙인다.
        loadOlderMessages: function() {
            if (this.loadingHistory || !this.hasMore || this.nextCursor === null) {
                return;
            }

            var _this = this;
            var container = this.$refs.messageContainer;
            // 앞에 메시지를 끼워 넣으면 스크롤 위치가 그만큼 밀린다.
            // 삽입 전후 높이 차이로 보정해 주지 않으면 화면이 튄다.
            var previousHeight = container.scrollHeight;
            var previousTop = container.scrollTop;

            this.loadingHistory = true;

            axios.get('/chat/room/' + this.roomId + '/messages', {
                params: { before: this.nextCursor, size: HISTORY_PAGE_SIZE }
            }).then(function(response) {
                var data = response.data;
                _this.messages = data.messages.concat(_this.messages);
                _this.nextCursor = data.nextCursor;
                _this.hasMore = data.hasMore;

                _this.$nextTick(function() {
                    container.scrollTop = container.scrollHeight - previousHeight + previousTop;
                });
            }).catch(function(error) {
                console.error("이전 메시지를 불러오지 못했습니다:", error);
            }).then(function() {
                _this.loadingHistory = false;
            });
        },

        // 메시지 영역 최상단 근처에 닿으면 다음 페이지를 요청한다.
        onScroll: function() {
            var container = this.$refs.messageContainer;
            if (container && container.scrollTop <= 0) {
                this.loadOlderMessages();
            }
        },

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

            // 내역 조회분과 실시간 수신분이 겹칠 수 있다. 저장된 메시지는 id가
            // 있으므로 그걸로 중복을 걸러낸다(입퇴장 알림은 저장하지 않아 id가 없다).
            if (recv.id !== null && recv.id !== undefined) {
                var exists = this.messages.some(function(m) {
                    return m.id === recv.id;
                });
                if (exists) {
                    return;
                }
            }

            this.$set(this.messages, this.messages.length, {
                "id": recv.id,
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
