  // WebSocket and STOMP initialization
  var sock = new SockJS("/ws-stomp");
  var ws = Stomp.over(sock);
  var reconnect = 0;

  // Vue.js instance
  var vm = new Vue({
      el: '#app',

      data: {
          roomId: '',
          room: {},
          roomName: '',
          sender: '',
          message: '',
          messages: []
      },

      created() {
          try {

              this.roomId = localStorage.getItem('wschat.roomId') || 'defaultRoomId';
              this.sender = localStorage.getItem('wschat.userNickname') || 'Guest';
              this.roomName = localStorage.getItem('wschat.teamName') || '기본 이름'

              this.findRoom();

          } catch (error) {
              console.error('Error during Vue initialization:', error);
          }
      },
      methods: {
          findRoom: function() {
              axios.get('/chat/room/' + this.roomId)
                .then(response => {
                    this.room = response.data;
                })
                .catch(error => {
                    console.error('Error fetching room data:', error);
                });
          },
          sendMessage: function() {
              if (this.sender.trim() === '') {
                  alert('Please enter your name before sending a message.');
                  return;
              }
              ws.send("/pub/chat/message", {}, JSON.stringify({
                  type: 'TALK',
                  roomId: this.roomId,
                  sender: this.sender,
                  message: this.message
              }));
              this.message = '';
          },
          recvMessage: function(recv) {
              this.messages.unshift({
                  type: recv.type,
                  sender: recv.type === 'ENTER' ? '[알림]' : recv.sender,
                  message: recv.message
              });
          }
      }
  });

  function connect() {
      ws.connect({}, function(frame) {
          ws.subscribe("/sub/chat/room/" + vm.$data.roomId, function(message) {
              var recv = JSON.parse(message.body);
              vm.recvMessage(recv);
          });
          ws.send("/pub/chat/message", {}, JSON.stringify({
              type: 'ENTER',
              roomId: vm.$data.roomId,
              sender: vm.$data.sender
          }));
      }, function(error) {
          console.error('WebSocket connection error:', error);
          if (reconnect++ <= 5) {
              setTimeout(function() {
                  console.log("Reconnecting...");
                  sock = new SockJS("/ws-stomp");
                  ws = Stomp.over(sock);
                  connect();
              }, 10 * 1000);
          }
      });
  }
  connect();