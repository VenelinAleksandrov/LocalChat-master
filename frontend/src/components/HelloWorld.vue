import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

export default {
name: "ChatApp",
data() {
return {
username: "",
newMessage: "",
messageHistory: [], // История на съобщенията
theme: "light",
stompClient: null, // WebSocket клиент
};
},
methods: {
connect() {
const socket = new SockJS('http://localhost:8080/chat');  // Свързване към WebSocket сървър
this.stompClient = new Client({
brokerURL: 'ws://localhost:8080/chat',
connectHeaders: {},
onConnect: () => {
this.stompClient.subscribe('/topic/messages', (messageOutput) => {
this.messageHistory.push(JSON.parse(messageOutput.body));  // Добавяме съобщението в историята
});
},
});
this.stompClient.activate();  // Стартираме WebSocket връзката
},

sendMessage() {
if (this.username && this.newMessage) {
const messageObject = {
username: this.username,
content: this.newMessage
};

// Изпращане на съобщението през WebSocket
this.stompClient.publish({
destination: '/app/send',  // Изпращане на съобщението
body: JSON.stringify(messageObject)
});

this.newMessage = "";  // Изчистваме полето за съобщения
}
},

toggleTheme() {
this.theme = this.theme === "light" ? "dark" : "light";
},
},
mounted() {
this.connect();  // Свързваме се с WebSocket при зареждане на компонента
}
};