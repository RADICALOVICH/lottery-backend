let adminToken = "";
let userToken = "";
let lastDrawId = "";

let lastLoggedOutAdminToken = "";
let lastLoggedOutUserToken = "";

function updateStateView() {
    document.getElementById("adminTokenView").textContent = adminToken || "не получен";
    document.getElementById("userTokenView").textContent = userToken || "не получен";
    document.getElementById("drawIdView").textContent = lastDrawId || "не выбран";

    document.getElementById("oldAdminTokenView").textContent = lastLoggedOutAdminToken || "не сохранён";
    document.getElementById("oldUserTokenView").textContent = lastLoggedOutUserToken || "не сохранён";

    if (lastDrawId) {
        document.getElementById("runDrawId").value = lastDrawId;
        document.getElementById("buyTicketDrawId").value = lastDrawId;
        document.getElementById("resultDrawId").value = lastDrawId;

        const buyWithoutTokenInput = document.getElementById("buyWithoutTokenDrawId");

        if (buyWithoutTokenInput) {
            buyWithoutTokenInput.value = lastDrawId;
        }
    }
}

function scrollToResult() {
    document.getElementById("resultPanel").scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}

function showLocalMessage(title, message) {
    scrollToResult();

    const requestInfo = document.getElementById("requestInfo");
    const statusInfo = document.getElementById("statusInfo");
    const output = document.getElementById("output");

    requestInfo.textContent = title;
    statusInfo.textContent = "local check";
    statusInfo.className = "muted";

    output.textContent = JSON.stringify({
        message: message
    }, null, 2);
}

async function sendRequest(method, endpoint, body = null, token = "") {
    scrollToResult();

    const requestInfo = document.getElementById("requestInfo");
    const statusInfo = document.getElementById("statusInfo");
    const output = document.getElementById("output");

    requestInfo.textContent = method + " " + endpoint;
    statusInfo.textContent = "loading...";
    statusInfo.className = "muted";
    output.textContent = "Выполняю запрос...";

    const headers = {
        "Accept": "application/json"
    };

    if (body !== null) {
        headers["Content-Type"] = "application/json";
    }

    if (token) {
        headers["Authorization"] = "Bearer " + token;
    }

    try {
        const response = await fetch(endpoint, {
            method: method,
            headers: headers,
            body: body === null ? null : JSON.stringify(body)
        });

        const contentType = response.headers.get("content-type");
        let data;

        if (contentType && contentType.includes("application/json")) {
            data = await response.json();
            output.textContent = JSON.stringify(data, null, 2);
        } else {
            data = await response.text();
            output.textContent = data;
        }

        statusInfo.textContent = "status: " + response.status;
        statusInfo.className = response.ok ? "ok" : "error";

        return {
            ok: response.ok,
            status: response.status,
            data: data
        };
    } catch (error) {
        statusInfo.textContent = "request failed";
        statusInfo.className = "error";
        output.textContent = error.message;

        return {
            ok: false,
            status: 0,
            data: null
        };
    }
}

function extractToken(data) {
    if (!data) {
        return "";
    }

    if (data.token) {
        return data.token;
    }

    if (data.accessToken) {
        return data.accessToken;
    }

    if (data.jwt) {
        return data.jwt;
    }

    return "";
}

async function loginAdmin() {
    const body = {
        login: document.getElementById("adminLogin").value,
        password: document.getElementById("adminPassword").value
    };

    const result = await sendRequest("POST", "/auth/login", body);
    const token = extractToken(result.data);

    if (result.ok && token) {
        adminToken = token;
        updateStateView();
    }
}

async function registerUser() {
    const login = document.getElementById("userRegisterLogin").value;
    const password = document.getElementById("userRegisterPassword").value;

    document.getElementById("userLogin").value = login;
    document.getElementById("userPassword").value = password;

    await sendRequest("POST", "/auth/register", {
        login: login,
        password: password
    });
}

async function loginUser() {
    const body = {
        login: document.getElementById("userLogin").value,
        password: document.getElementById("userPassword").value
    };

    const result = await sendRequest("POST", "/auth/login", body);
    const token = extractToken(result.data);

    if (result.ok && token) {
        userToken = token;
        updateStateView();
    }
}

async function createDraw() {
    const body = {
        title: document.getElementById("drawTitle").value,
        endDate: document.getElementById("drawEndDate").value,
        totalTickets: Number(document.getElementById("drawTotalTickets").value)
    };

    const result = await sendRequest("POST", "/admin/draws", body, adminToken);

    if (result.ok && result.data && result.data.id) {
        lastDrawId = result.data.id;
        updateStateView();
    }
}

async function buyTicket() {
    const drawId = document.getElementById("buyTicketDrawId").value;

    if (!drawId) {
        showLocalMessage("Покупка билета", "Сначала укажи drawId или создай тираж через кнопку «Создать тираж».");
        return;
    }

    await sendRequest("POST", "/draws/" + drawId + "/tickets", null, userToken);
}

async function runDraw() {
    const drawId = document.getElementById("runDrawId").value;

    if (!drawId) {
        showLocalMessage("Запуск розыгрыша", "Сначала укажи drawId или создай тираж через кнопку «Создать тираж».");
        return;
    }

    await sendRequest("POST", "/admin/draws/" + drawId + "/run-draw", null, adminToken);
}

async function getDrawResult() {
    const drawId = document.getElementById("resultDrawId").value;

    if (!drawId) {
        showLocalMessage("Результат тиража", "Сначала укажи drawId или создай тираж через кнопку «Создать тираж».");
        return;
    }

    await sendRequest("GET", "/draws/" + drawId + "/result");
}

async function logoutUser() {
    if (!userToken) {
        showLocalMessage(
            "POST /auth/logout",
            "User token не получен. Сначала нажми «Войти как юзер», потом «Выйти как юзер»."
        );
        return;
    }

    const tokenToLogout = userToken;
    const result = await sendRequest("POST", "/auth/logout", null, tokenToLogout);

    if (result.ok) {
        lastLoggedOutUserToken = tokenToLogout;
        userToken = "";
        updateStateView();
    }
}

async function logoutAdmin() {
    if (!adminToken) {
        showLocalMessage(
            "POST /auth/logout",
            "Admin token не получен. Сначала нажми «Войти как админ», потом «Выйти как админ»."
        );
        return;
    }

    const tokenToLogout = adminToken;
    const result = await sendRequest("POST", "/auth/logout", null, tokenToLogout);

    if (result.ok) {
        lastLoggedOutAdminToken = tokenToLogout;
        adminToken = "";
        updateStateView();
    }
}

async function checkOldUserToken() {
    if (!lastLoggedOutUserToken) {
        showLocalMessage(
            "GET /users/me",
            "Старый user token ещё не сохранён. Сначала нажми «Выйти как юзер»."
        );
        return;
    }

    await sendRequest("GET", "/users/me", null, lastLoggedOutUserToken);
}

async function checkOldAdminToken() {
    if (!lastLoggedOutAdminToken) {
        showLocalMessage(
            "GET /admin/ping",
            "Старый admin token ещё не сохранён. Сначала нажми «Выйти как админ»."
        );
        return;
    }

    await sendRequest("GET", "/admin/ping", null, lastLoggedOutAdminToken);
}

async function buyTicketWithoutToken() {
    const drawId = document.getElementById("buyWithoutTokenDrawId").value;

    if (!drawId) {
        showLocalMessage(
            "POST /draws/{id}/tickets",
            "Сначала укажи drawId или создай тираж через кнопку «Создать тираж»."
        );
        return;
    }

    await sendRequest("POST", "/draws/" + drawId + "/tickets");
}

updateStateView();