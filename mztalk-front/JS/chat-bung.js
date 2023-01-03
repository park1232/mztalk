const socket = io();

window.addEventListener('load', main);

// window가 로드 완료된 후 최초 진입
async function main() {
    // 현재 창의 Query string으로부터 '나'의 userId 가져오기
    const meUserId = new URLSearchParams(location.search).get('userId');

    // 1:1 채팅에서 상대가 '나'의 소켓으로 메시지를 발송할 수 있도록 하기 위해서
    // '나'의 User ID와 소켓 ID를 서버가 맵핑할 수 있도록 subscribe 함
    socket.emit('subscribe', ({ userId: meUserId }));

    //#region 맞팔 유저 목록 가져오기
    const followListRepository = new FollowListRepository();
    const followList = await followListRepository.fetch(meUserId);
    const followedUserIds = followList.data.map(({ toUserId }) => toUserId);
    //#endregion

    //#region 맞팔 유저 목록의 유저 정보 가져오기
    const userRepository = new UserRepository();
    const users = await Promise.all(
        followedUserIds.map((userId) => userRepository.fetch(userId)),
    );
    //#endregion

    //#region 각 유저별 이전 대화 가져오기
    const conversationRepository = new ConversationRepository(meUserId);
    const usersConversations = await Promise.all(users.map((user) => conversationRepository.fetch(user.userId)));
    //#endregion

    //#region 메시지 박스 컨트롤러 초기화
    const messageBoxController = new MessageBoxController();

    // 위에서 가져온 유저 리스트와 대화 정보를 DOM으로 빌드하여 UI에 유저 리스트 렌더링
    messageBoxController.setUserList(users, usersConversations);
    //#endregion

    //#region 로딩 시 읽지 않은 메시지가 있는 유저 하이라이팅
    users.forEach((_, index) => {
        const latestConversation = usersConversations[index].at(-1);

        // 마지막으로 보낸 메시지가 '나'로부터 출발했다면
        // 마지막까지 읽었다는 의미이므로 아무 작업도 하지 않음
        if (!latestConversation || latestConversation.fromUserId === meUserId) {
            return;
        }
        
        if (!latestConversation.read) {
            const { fromUserId, targetUserId, message, sendDate } = latestConversation;

            // 페이지 최초 로드 시 마지막 대화 인디케이팅을 따로 구현하지 않고,
            // 접속 중 메시지가 온 상황을 재현하기 위해
            // 메시지 데이터를 기반으로 onReceiveMessage를 그대로 실행함
            messageBoxController.onReceiveMessage({ fromUserId, targetUserId, message, sendDate });
        }
    });
    //#endregion

    //#region 채팅 박스 컨트롤러 초기화
    // 이후 테스트 코드 작성 및 인터페이스 변경 등을 염두에 두고
    // DI 패턴으로 conversationRepository 인스턴스를 주입함
    const chatBoxController = new ChatBoxController(meUserId, conversationRepository);
    //#endregion

    //#region 이벤트 핸들러 설정
    chatBoxController.chatFormDom.addEventListener('submit', chatBoxController.sendMessage);

    // 메시지 수신 이벤트 처리
    socket.on('receiveMessage', async ({ fromUserId, targetUserId, message, sendDate }) => {
        await chatBoxController.onReceiveMessage({ fromUserId, targetUserId, message, sendDate });

        messageBoxController.onReceiveMessage({ fromUserId, targetUserId, message, sendDate });
    });

    // 특정 유저 클릭 시 대화방 열기
    for (const userElement of messageBoxController.userElements) {
        // 클릭한 유저가 누구인지 판단할 수 있도록 각 유저별로 이벤트를 구분
        userElement.addEventListener('click', () => {
            messageBoxController.selectUser(userElement, users);
            chatBoxController.openChat(messageBoxController.selectedUser);
        }); 
    }
    //#endregion
}

/**
 * ### `#messageBox` DOM 컨트롤러
 * - 유저 리스트 UI 렌더링
 * - 유저 클릭 시 채팅창을 표시할 유저 선택
 */
class MessageBoxController {
    #messageBoxDom = document.getElementById('messageBox');

    #users = [];
    #selectedUser = {};

    // 외부에서 이벤트를 등록해야 하기 때문에 DOM 객체 내보내기
    get messageBoxDom() {
        return this.#messageBoxDom;
    }

    // 현재 렌더링 된 모든 유저 element를 배열로 반환
    get userElements() {
        return [...this.#messageBoxDom.children];
    }

    get selectedUser() {
        return this.#selectedUser;
    }

    // 좌측 유저 리스트 영역에 유저 목록 설정
    // 마지막 대화 내용과 시각을 표기해야 하기 때문에 messages도 같이 받음
    setUserList(users, messages) {
        this.#users = users;

        /**
         * this.onReceiveMessage에서 innerHTML를 설정하여 DOM 변경을 하는데,
         * innerHTML 특성상 대상 해당 Element의 자식으로 DOM 객체 넣기 때문에,
         * 최초 로드 시 div로 감싸 빌드하고,
         * 이후 DOM 변경 시에는 div를 root로 하여 innerHTML을 설정하도록 함
         * * outerHTML를 사용하게 되면 해당 DOM 객체가 초기화 되어,
         * * 이벤트 핸들러도 사라져서 main() 함수에서 정의한 유저 선택 등의 기능이 동작하지 않기 때문에,
         * * 반드시 innerHTML을 사용해야 함
         */
        const userHtmls = users.map((user, index) => `
            <div>
                ${ this.#buildUserHtml(user, messages[index].at(-1)) }
            </div>
        `);

        // 리스트에 회원 정보 뿌려주기
        this.#messageBoxDom.innerHTML += userHtmls.join('\n');
    }

    // 유저 클릭 시 해당 유저를 현재 선택된 유저로 설정
    selectUser(userElement, users) {
        // 메시지 읽음 처리
        userElement.className = userElement.className
            .replace('border', '')
            .replace('border-primary', '')
            .trim();

        // 클릭한 유저 객체를 찾기 위해 모든 유저의 element 중 현재 클릭된 element의 인덱스를 찾음
        const clickedIndex = this.userElements.findIndex((node) => node === userElement);
    
        if (clickedIndex < 0) {
            return;
        }

        // 클릭한 유저 객체를 현재 선택된 유저로 설정
        this.#selectedUser = users[clickedIndex];
    }

    // 메시지를 받았을 때 좌측 유저 리스트에서 동작할 기능 구현
    onReceiveMessage({ fromUserId, message, sendDate }) {
        const userIndex = this.#users.findIndex(({ userId }) => userId === fromUserId);
        const userElement = this.userElements[userIndex];

        //#region 새로운 메시지 알림
        // 메시지를 보낸 사람과의 채팅창이 열려 있지 않을 때만 인디케이팅
        if (fromUserId !== this.selectedUser.userId) {
            // 인디케이터 클래스가 중복으로 적용되지 않도록, 읽지 않은 메시지가 없었던 경우에만 적용
            if (!userElement.className.includes('border')) {
                userElement.className += 'border border-primary';
            }
        }

        // 변경이 일어난 유저의 element만 변경
        userElement.innerHTML = this.#buildUserHtml(this.#users[userIndex], { message, sendDate });
        //#endregion
    }

    // 각 유저 데이터를 마지막 메시지 내용 및 시각과 함께 DOM으로 렌더링
    #buildUserHtml(user, latestMessage) {
        return `
            <a href="#" class="list-group-item list-group-item-action list-group-item-light rounded-0">
                <div class="media">
                    <img src="${user.imageUrl}" alt="user" width="50" class="rounded-circle" />

                    <div class="media-body ml-4">
                        <div class="d-flex align-items-center justify-content-between mb-1">
                            <h6 class="mb-0">${user.nickname}</h6>
                            <small class="small font-weight-bold">${ latestMessage ? dayjs(latestMessage.sendDate).format('HH:mm') : '' }</small>
                        </div>

                        <p class="font-italic text-muted mb-0 text-small">${ latestMessage ? latestMessage.message : '' }</p>
                    </div>
                </div>
            </a>
        `;
    }
}

/**
 * ### `#chatBox` DOM 컨트롤러
 * - 채팅창 UI 렌더링
 * - 대화 전송/수신 처리
 */
class ChatBoxController {
    #chatBoxDom = document.getElementById('chatBox');
    #chatFormDom = document.getElementById('form');
    #chatInputDom = document.getElementById('input');

    #selectedUser = {};

    // 외부에서 이벤트를 등록해야 하기 때문에 DOM 객체 내보내기
    get chatFormDom() {
        return this.#chatFormDom;
    }

    constructor(meUserId, conversationRepository) {
        this.meUserId = meUserId;
        this.conversationRepository = conversationRepository;

        this.sendMessage = this.sendMessage.bind(this);
        this.onReceiveMessage = this.onReceiveMessage.bind(this);
    }

    // 특정 유저와의 채팅창 열기
    // 현재 상대와의 대화 읽음 처리, 이전에 나눈 대화 렌더링 등의 작업을 수행함
    async openChat(targetUser) {
        this.#selectedUser = targetUser;

        const conversations = await this.conversationRepository.fetch(this.#selectedUser.userId);

        // 현재 상대와의 대화 읽음 처리
        await this.conversationRepository.read(this.#selectedUser.userId);

        // 기존 대화 보여주기
        this.#chatBoxDom.innerHTML = conversations
            .map(({ fromUserId, message, sendDate }) => {
                // 메시지의 `fromUserId`를 기준으로 메시지의 출발 유저를 '나'와 상대로 구분하여 렌더링
                switch (fromUserId) {
                    case this.meUserId:
                        return this.#buildSendChatHtml(message, sendDate);
                    case this.#selectedUser.userId:
                        return this.#buildReceiveChatHtml(message, sendDate);
                    default:
                        return '';
                }
            })
            .join('\n');

        this.#chatBoxDom.scrollTop = chatBox.scrollHeight;
    }

    // 메시지 전송 후 메시지 렌더링
    sendMessage(event) {
        event.preventDefault();

        if (!input.value) {
            return;
        }

        const current = new Date();

        // 소켓 서버에 sendMessage 발행
        socket.emit('sendMessage', {
            fromUserId: this.meUserId,
            targetUserId: this.#selectedUser.userId,
            message: input.value,
            sendDate: current.toISOString(),
        });

        // '나'가 보낸 메시지로 말풍선 렌더링
        this.#chatBoxDom.innerHTML += this.#buildSendChatHtml(input.value, current);
        this.#chatBoxDom.scrollTop = chatBox.scrollHeight;

        this.#chatInputDom.value = '';
    }

    // 메시지 수신 시 메시지 렌더링
    async onReceiveMessage({ fromUserId, targetUserId, message, sendDate }) {
        console.log('onReceiveMessage', { fromUserId, targetUserId, message, sendDate });

        // 현재 메시지를 보낸 사람과의 채팅창이 아니라면 아무 동작도 하지 않음
        if (this.#selectedUser.userId !== fromUserId) {
            return;
        }
        // 이후로는 현재 메시지를 보낸 사람과의 채팅창의 동작

        // 현재 선택된 유저와 나눈 모든 메시지를 읽음 처리함
        await this.conversationRepository.read(fromUserId);

        // 상대가 보낸 메시지로 말풍선 렌더링
        this.#chatBoxDom.innerHTML += this.#buildReceiveChatHtml(message, sendDate);
        this.#chatBoxDom.scrollTop = this.#chatBoxDom.scrollHeight;
    }
    
    // '나'가 상대에게 보낸 메시지 DOM 빌드
    #buildSendChatHtml(message, sendDate) {
        return `
            <div class="media w-50 ml-auto mb-3">
                <div class="media-body">
                    <div class="bg-primary rounded py-2 px-3 mb-2">
                        <p class="text-small mb-0 text-white">${ message }</p>
                    </div>

                    <p class="small text-muted">${ dayjs(sendDate).format('HH:mm') }</p>
                </div>
            </div>
        `;
    }

    // 상대가 '나'에게 보낸 메시지 DOM 빌드
    #buildReceiveChatHtml(message, sendDate) {
        return `
            <div class="media w-50 mb-3">
                <img src="${this.#selectedUser.imageUrl}" alt="${this.#selectedUser.userId}" width="50" class="rounded-circle" />

                <div class="media-body ml-3">
                    <div class="bg-light rounded py-2 px-3 mb-2">
                        <p class="text-small mb-0 text-muted">${message}</p>
                    </div>

                    <p class="small text-muted">${ dayjs(sendDate).format('HH:mm') }</p>
                </div>
            </div>
        `;
    }
}

/**
 * '나'의 `userId`를 기준으로 맞팔 상태인 유저 목록을 가져오는 API
 */
class FollowListRepository {
    async fetch(userId) {
        const response = await fetch(`http://localhost:8000/story/matpalList/${userId}`, {
            method: "GET",
            headers: {
                "content-type": "application/json"
            },
        })

        return response.json();
    }
}

/**
 * `userId`를 기준으로 유저의 상세 정보를 가져오는 API
 */
class UserRepository {
    async fetch(userId) {
        const response = await fetch(`http://localhost:8000/login/user-info/${userId}`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
            },
        })

        return response.json();
    }
}

/**
 * 이전 대화 목록 가져오는 API
 */
class ConversationRepository {
    constructor(fromUserId) {
        this.fromUserId = fromUserId;
    }

    async fetch(targetUserId) {
        const query = { targetUserId };
        const response = await fetch(`http://localhost:3000/conversations${ this.#buildQueryString(query) }`, {
            method: 'GET',
            headers: { userId: this.fromUserId },
        });

        return response.json();
    }

    // 특정 상대와 나눈 메시지 읽음 처리
    async read(targetUserId) {
        const query = { targetUserId };
        const response = await fetch(`http://localhost:3000/conversations/read${ this.#buildQueryString(query) }`, {
            method: 'PUT',
            headers: { userId: this.fromUserId },
        });

        return response.json();
    }

    #buildQueryString(query) {
        return `?${ Object.entries(query).map(([key, value]) => `${key}=${value}`).join('&') }`;
    }
}
