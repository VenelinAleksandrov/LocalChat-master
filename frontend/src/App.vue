<template>
  <div id="app">
    <!-- Регистрация -->
    <div v-if="!isLoggedIn && !isLogin" class="auth-container">
      <h2>Регистрация</h2>
      <input v-model="registerUsername" placeholder="Username">
      <input v-model="registerPassword" type="password" placeholder="Password" @input="validatePassword">

      <div class="password-requirements">
        <span :class="{ valid: passwordLength }">✓ Минимум 8 букви</span>
        <span :class="{ valid: hasUppercase }">✓ 1 Главна буква</span>
        <span :class="{ valid: hasNumber }">✓ 1 число</span>
      </div>

      <h3>Избери аватар</h3>
      <div class="avatar-selection">
        <img
            v-for="(avatar, index) in avatars"
            :key="index"
            :src="avatar"
            :alt="'Avatar ' + index"
            :class="['avatar', { selected: selectedAvatar === avatar }]"
            @click="selectAvatar(avatar)"
        >
      </div>

      <button @click="register">Регистрация</button>
      <div class="register-link">
        Вече имаш профил? <a href="#" @click.prevent="switchToLogin">Вход</a>
      </div>
    </div>

    <!-- Вход -->
    <div v-if="!isLoggedIn && isLogin" class="auth-container">
      <h2>Вход</h2>
      <input v-model="loginUsername" placeholder="Username">
      <input v-model="loginPassword" type="password" placeholder="Password">
      <button @click="login">Влизане</button>
      <p>
        Нямаш акаунт?
        <a href="#" @click.prevent="switchToRegister">Регистрирай се тук</a>
      </p>
    </div>

    <!-- Чат -->
    <div v-if="isLoggedIn" class="chat-container">
      <div class="header-container">
        <h1>Welcome to Hidden Chat, {{ username }}!</h1>
        <button @click="logout" class="logout-btn">Изход</button>
      </div>

      <div class="messages">
        <div
            v-for="(message, index) in messageHistory"
            :key="index"
            :class="['message', message.isFromCurrentUser ? 'sent' : 'received']"
        >

          <img v-if="message.avatar" :src="message.avatar" alt="User avatar" class="message-avatar">

          <div class="message-content">
            <div class="message-header">
              <strong>{{ message.username }}:</strong>
              <span class="message-text">{{ message.content }}</span>
            </div>
            <span class="timestamp">{{ message.timestamp }}</span>
          </div>
        </div>
      </div>

      <div class="input-container">
        <input
            v-model="newMessage"
            class="message-input"
            placeholder="Type a message..."
            @keyup.enter="sendMessage"
        >
        <button @click="sendMessage">Изпрати</button>
      </div>
    </div>
  </div>
</template>

<script>
import SockJS from "sockjs-client";
import {Client} from "@stomp/stompjs";

export default {
  data() {
    return {
      registerUsername: "",
      registerPassword: "",
      loginUsername: "",
      loginPassword: "",
      selectedAvatar: "",
      avatars: [
        "https://api.dicebear.com/9.x/adventurer-neutral/svg?seed=Adrian",
        "https://api.dicebear.com/9.x/adventurer-neutral/svg?seed=Luis",
        "https://api.dicebear.com/9.x/adventurer-neutral/svg?seed=Brian",
        "https://api.dicebear.com/9.x/adventurer-neutral/svg?seed=Andrea",
        "https://api.dicebear.com/9.x/adventurer-neutral/svg?seed=Jameson"
      ],
      username: localStorage.getItem("username") || "",
      isLoggedIn: false,
      isLogin: false,
      passwordLength: false,
      hasUppercase: false,
      hasNumber: false,
      newMessage: "",
      messageHistory: []
    };
  },
  methods: {
    selectAvatar(avatar) {
      this.selectedAvatar = avatar;
    },
    validatePassword() {
      this.passwordLength = this.registerPassword.length >= 8;
      this.hasUppercase = /[A-Z]/.test(this.registerPassword);
      this.hasNumber = /\d/.test(this.registerPassword);
    },
    switchToLogin() {
      this.isLogin = true;
    },
    switchToRegister() {
      this.isLogin = false;
    },

    async register() {
      if (this.registerUsername && this.registerPassword && this.selectedAvatar) {
        try {
          const response = await fetch("http://localhost:8080/api/users/register", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
              username: this.registerUsername,
              password: this.registerPassword,
              avatar: this.selectedAvatar
            })
          });

          const result = await response.json();

          if (result.success) {
            this.username = this.registerUsername;
            this.isLoggedIn = true;
            localStorage.setItem("username", this.username);
            localStorage.setItem("avatar", this.selectedAvatar);
            this.connect();
          } else {
            alert(result.message);
          }
        } catch (error) {
          alert("Registration failed: " + error);
        }
      } else {
        alert("Please fill all fields and select an avatar!");
      }
    },

    async login() {
      if (this.loginUsername && this.loginPassword) {
        try {
          const response = await fetch("http://localhost:8080/api/auth/login", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
              username: this.loginUsername,
              password: this.loginPassword
            })
          });

          const result = await response.json();

          if (result.success) {
            this.username = this.loginUsername;
            this.isLoggedIn = true;
            localStorage.setItem("username", this.username);


            if (result.sessionId) {
              localStorage.setItem("sessionId", result.sessionId);
            }

            if (result.avatar) {
              this.selectedAvatar = result.avatar;
              localStorage.setItem("avatar", result.avatar);
            }

            this.connect();
          } else {
            alert(result.message);
          }
        } catch (error) {
          alert("Login failed: " + error);
        }
      }
    },
    async logout() {
      try {
        const sessionId = localStorage.getItem('sessionId');

        console.log('Attempting logout with sessionId:', sessionId);

        const response = await fetch('http://localhost:8080/api/auth/logout', {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${sessionId}`,
            'Content-Type': 'application/json'
          }
        });


        console.log('Logout response status:', response.status);

        if (!response.ok) {
          const errorData = await response.json();
          console.error('Logout failed:', errorData);
          throw new Error(errorData.message || 'Logout failed');
        }


        localStorage.removeItem('sessionId');
        localStorage.removeItem('username');
        localStorage.removeItem('avatar');
        this.isLoggedIn = false;

        if (this.stompClient) {
          this.stompClient.deactivate();
        }


        this.isLogin = true;

      } catch (error) {
        console.error('Logout error:', error);
        alert(error.message || 'Failed to logout');
      }
    }
    ,
    connect() {
      const socket = new SockJS('http://localhost:8080/chat-websocket');
      this.stompClient = new Client({
        webSocketFactory: () => socket,
        debug: (str) => console.log('STOMP: ' + str),
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log('Successfully connected to WebSocket');

          this.stompClient.subscribe("/topic/messages", (message) => {
            const msg = JSON.parse(message.body);
            if (msg.username !== this.username) {
              msg.isFromCurrentUser = false;
              this.messageHistory.push(msg);
            }
          });

        },
        onDisconnect: () => {
          console.log('Disconnected from WebSocket');
        }
      });

      this.stompClient.activate();
    },

    sendMessage() {
      if (!this.newMessage.trim()) return;

      const message = {
        username: this.username,
        content: this.newMessage,
        avatar: localStorage.getItem("avatar") || "",
        timestamp: new Date().toLocaleTimeString(),
        isFromCurrentUser: true
      };

      this.messageHistory.push(message);

      this.stompClient.publish({
        destination: "/app/send",
        body: JSON.stringify(message)
      });

      this.newMessage = "";
    }
  }
}


</script>

<style>
/* suppress warnings */
.sent, .received, .selected {
}

#app {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #eaeaea;
}

body, html {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.auth-container {
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  background: white;
  width: 450px;
  height: 500px;
  text-align: center;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.auth-container input,
.auth-container button {
  display: block;
  width: 100%;
  max-width: 300px;
  margin: 10px 0;
}

input {
  width: 100%;
  max-width: 300px;
  padding: 10px;
  margin: 10px 0;
  border-radius: 5px;
  border: 1px solid #ccc;
}

.message-input {
  width: 100%;
  max-width: 700px;
  padding: 10px;
  font-size: 16px;
  border-radius: 10px;
  border: 1px solid #ccc;
}

.password-requirements span {
  display: block;
  color: red;
  font-size: 15px;
  margin: 15px 0;
}

.password-requirements .valid {
  color: green;
}

.avatar-selection {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin: 15px 0;
}

.avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;
  transition: transform 0.3s ease;
  margin-right: 8px;
}

.avatar:hover {
  transform: scale(1.3);
  border-color: #007bff;
}

.avatar.selected {
  border-color: #007bff;
}

button {
  padding: 10px 20px;
  border: none;
  background: #007bff;
  color: white;
  cursor: pointer;
  border-radius: 5px;
  margin-top: 15px;
}

button:hover {
  background: #0056b3;
}

.chat-container {
  display: flex;
  flex-direction: column;
  height: 80vh;
  width: 80vw;
  max-width: 800px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  display: flex;
  flex-direction: column;
}

.message {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  max-width: 80%;
  padding: 10px;
  margin: 5px 0;
  animation: fadeIn 0.3s ease-in-out;
}


@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.sent {
  background-color: #007bff;
  color: white;
  align-self: flex-end;
  border-radius: 18px 18px 0 18px;
  /* Добавете: */
  flex-direction: row; /* Винаги ред отляво надясно */
}

.received {
  background-color: #e9ecef;
  color: black;
  align-self: flex-start;
  border-radius: 18px 18px 18px 0;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 10px;
  object-fit: cover;
  flex-shrink: 0; /* Предотвратява свиване на аватара */
}

.sent .message-avatar {
  margin-right: 0;
  margin-left: 10px;
}

.message-content {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  min-width: 0; /* Позволява правилно свиване */
}

.message-content strong {
  margin-bottom: 5px;
}

.sent .message-content strong {
  color: #cce0ff;
}

.received .message-content strong {
  color: #333;
}

.message-header {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.message-header {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.message-text {
  word-break: break-word;
  white-space: pre-wrap;
}
.input-container {
  display: flex;
  width: 100%;
  padding: 10px;
  border-top: 1px solid #ddd;
}

.timestamp {
  font-size: 0.75em;
  opacity: 0.8;
  margin-top: 4px;
}

.sent .timestamp {
  color: #e0e0e0;
}

.received .timestamp {
  color: #666;
}

.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background-color: white;
  border-bottom: 1px solid #ddd;
}

.logout-btn {
  background-color: #dc3545;
  color: white;
  border: none;
  padding: 5px 10px;
  font-size: 14px;
  cursor: pointer;
  border-radius: 5px;
}

.register-link a {
  text-decoration: none;
  color: blue;
  font-weight: bold;
  cursor: pointer;
}

.register-link a:hover {
  text-decoration: underline;
}

.register-link {
  margin-top: 20px;
  display: block;
  text-align: center;
}
</style>