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
          token: ''
      },
      created() {
          try {
              //roomId, roomName 등을 서버에서 부터 불러온다
              this.roomId = localStorage.getItem('wschat.roomId') || 'defaultRoomId';
              this.roomName = localStorage.getItem('wschat.teamName') || '기본 이름'
              var _this = this;

              //접속중인 계정의 Jwt Token을 호출해서 사용자 정보를 가져오고,
              //JWT 토큰을 발급받아서 WebSocket 연결에 사용한다.
              axios.get('chat/user').then(response => {
                //서버에서 가져온 유저 데이터
                _this.token = response.data.token;

                // 서버로 연결을 시도하고,
                ws.connect({"token":_this.token},function(frame){
                //연결이 완료되면 ws.subscribe()로 해당 채팅방을 구독한다.
                //"/sub/chat/room/" + _this.roomId 경로로 들어오는 메시지를 실시간으로 받아서 처리
                    ws.subscribe("/sub/chat/room"+ _this.roomId, function(message){
                        var recv = JSON.parse(message.body);
                        _this.recvMessage(recv);
                    });
                    _this.sendMessage('ENTER');
                }, function(error){
                alert("서버 연결에 실패 했습니다. 다시 접속해 주세요.");
                location.href="/chat/room";
                });
              });
      },
      methods: {
       //사용자가 메시지를 입력하고 '보내기' 버튼을 클릭하면
       //sendMessage ('TALK') 메서드가 실행
       // 사용자가 입력한 메시지와 메시지 타입을 WebSocket을 통해 서버로 전송
       // 그럼 서버 측에서 이 메시지를 Redis로 발행 (publish) 하고, 채팅방을 구독하고 있는 클라이언트에게
       // 메시지를 전달한다.
        sendMessage: function(type) {
            ws.send("/pub/chat/message", {"token":this.token}, JSON.stringify({type:type, roomId:this.roomId, message:this.message}));
            this.message = '';
                        },
        //새로 수신된 메시지를 화면에 추가한다.
        recvMessage: function(recv) {
            this.messages.unshift({"type":recv.type,"sender":recv.sender,"message":recv.message})
                        }
            }
      }
    });