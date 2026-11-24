// ---------- базовый API (с куками) ----------
async function api(path, opts = {}) {
    const res = await fetch(path, {
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        ...opts,
    });
    if (res.status === 204) return null;
    const text = await res.text();
    const data = text ? JSON.parse(text) : null;
    if (!res.ok) {
        const msg = (data && (data.message || data.error)) || res.statusText;
        throw new Error(msg);
    }
    return data;
}

// ---------- состояние ----------
let ME = null;

async function refreshMe() {
    try {
        const res = await api("/api/auth/me");
        // Если бэкенд возвращает просто строку или объект без поля user, адаптируем:
        ME = res ? (res.user ? res : { user: res, isAdmin: false }) : null;
    }
    catch { ME = null; }
    refreshHeader();
}

function refreshHeader() {
    const label = document.getElementById("current-user");
    const adminLink = document.getElementById("nav-admin");

    const loginBtn = document.getElementById("nav-login-btn");
    const registerBtn = document.getElementById("nav-register-btn");
    const logoutBtn = document.getElementById("nav-logout-btn");

    if (!ME) {
        // Если НЕ залогинен
        label.textContent = "Не выполнен вход";
        adminLink.style.display = "none";

        // Принудительно через style, игнорируя CSS
        loginBtn.style.display = "inline-block";
        registerBtn.style.display = "inline-block";
        logoutBtn.style.display = "none";
        return;
    }

    // Если ЗАЛОГИНЕН
    const username = ME.user.username || ME.user.name || ME.user.login || "Пользователь";
    label.textContent = `${username}${ME.isAdmin ? " (админ)" : ""}`;
    adminLink.style.display = ME.isAdmin ? "" : "none";

    // Принудительно через style
    loginBtn.style.display = "none";
    registerBtn.style.display = "none";
    logoutBtn.style.display = "inline-block";
}

function requireUser() {
    if (!ME) { openUserAuth("login"); return false; }
    return true;
}

// ---------- Модалки (Единая авторизация) ----------
function openUserAuth(mode) {
    const modal = document.getElementById("user-modal");
    const title = document.getElementById("user-modal-title");
    document.getElementById("user-login-box").style.display = "none";
    document.getElementById("user-register-box").style.display = "none";

    if (mode === "register") {
        title.textContent = "Регистрация";
        document.getElementById("user-register-box").style.display = "flex";
        document.getElementById("reg-username").focus();
    } else {
        title.textContent = "Вход";
        document.getElementById("user-login-box").style.display = "flex";
        document.getElementById("login-username").focus();
    }
    modal.classList.add("active");
}

function closeUserAuth() {
    document.getElementById("user-modal").classList.remove("active");
}

async function doRegister() {
    const username = document.getElementById("reg-username").value.trim();
    const email = document.getElementById("reg-email").value.trim();
    const password = document.getElementById("reg-password").value;
    try {
        await api("/api/auth/register", { method: "POST", body: JSON.stringify({ username, email, password }) });
        await refreshMe();
        toast("Регистрация успешна");
        closeUserAuth();
        location.hash = "#/quizzes";
        render();
    } catch (e) { toast(e.message); }
}

async function doLogin() {
    const username = document.getElementById("login-username").value.trim();
    const password = document.getElementById("login-password").value;
    try {
        await api("/api/auth/login", { method: "POST", body: JSON.stringify({ username, password }) });
        await refreshMe();

        if (ME.isAdmin) {
            toast(`Добро пожаловать, администратор ${ME.user.username}`);
            location.hash = "#/admin";
        } else {
            toast(`Добро пожаловать, ${ME.user.username}`);
            location.hash = "#/quizzes";
        }

        closeUserAuth();
        render();
    } catch (e) { toast(e.message); }
}

async function doLogout() {
    try { await api("/api/auth/logout", { method: "POST" }); } catch {}
    ME = null;
    refreshHeader();
    toast("Вы вышли");
    location.hash = "#/quizzes";
    render();
}

// ---------- Утилиты ----------
const view = document.getElementById("view");

function el(tag, attrs = {}, ...children) {
    const node = document.createElement(tag);
    for (const [k, v] of Object.entries(attrs)) {
        if (v === undefined || v === null) continue;
        if (k === "class") node.className = v;
        else if (k === "style") node.style.cssText = v;
        else if (k.startsWith("on")) node.addEventListener(k.slice(2), v);
        else if (k === "checked") node.checked = true;
        else if (k === "disabled") node.disabled = true;
        else node.setAttribute(k, v);
    }
    for (const c of children) {
        if (c == null) continue;
        node.appendChild(typeof c === "string" ? document.createTextNode(c) : c);
    }
    return node;
}

function toast(msg) {
    const t = document.getElementById("toast");
    t.textContent = msg;
    t.classList.add("show");
    clearTimeout(t._h);
    t._h = setTimeout(() => t.classList.remove("show"), 2500);
}

function fmtDate(s) {
    if (!s) return "—";
    return new Date(s).toLocaleString("ru-RU");
}

function setActive(page) {
    document.querySelectorAll(".nav a").forEach((a) => {
        a.classList.toggle("active", a.dataset.page === page);
    });
}

function parseHash() {
    const h = location.hash.replace(/^#\/?/, "");
    if (!h) return { name: "quizzes" };
    const [name, ...rest] = h.split("/");
    return { name, params: rest };
}

async function render() {
    view.innerHTML = "";
    const { name, params } = parseHash();
    try {
        if (pages[name]) await pages[name](...params);
        else view.appendChild(el("div", { class: "empty" }, "Страница не найдена"));
    } catch (e) {
        view.appendChild(el("div", { class: "empty" }, `Ошибка: ${e.message}`));
    }
}

window.addEventListener("hashchange", render);
window.addEventListener("DOMContentLoaded", async () => {
    await refreshMe();
    if (!location.hash) location.hash = "#/quizzes";
    else render();
});

// ---------- Страницы ----------
const pages = {};

// --- Викторины ---
pages.quizzes = async () => {
    setActive("quizzes");
    const data = await api("/api/quizzes?size=50");
    const list = data.content || [];

    let completedQuizIds = new Set();
    if (ME) {
        try {
            const history = await api(`/api/users/${ME.user.id}/history?size=1000`);
            (history.content || []).forEach(r => {
                if (r.status === "COMPLETED" || r.status === "TIMED_OUT") completedQuizIds.add(r.quiz.id);
            });
        } catch (e) {}
    }

    view.appendChild(el("h1", {}, "Викторины"));
    view.appendChild(el("p", {}, "Выберите викторину, чтобы пройти её."));

    if (!list.length) {
        view.appendChild(el("div", { class: "empty" }, "Пока нет опубликованных викторин"));
        return;
    }

    const box = el("div", { class: "list" });
    for (const q of list) {
        const isDone = completedQuizIds.has(q.id);
        box.appendChild(el("div", { class: "card" },
            el("div", { class: "row between" },
                el("div", {},
                    el("h3", {}, q.title),
                    el("div", { class: "muted" }, q.description || ""),
                    el("div", { class: "row", style: "margin-top:8px;gap:6px" },
                        el("span", { class: "tag" }, q.category?.name || "—"),
                        el("span", { class: "tag" }, q.difficulty),
                        q.timeLimitSeconds ? el("span", { class: "tag" }, `${q.timeLimitSeconds} сек`) : null,
                        isDone ? el("span", { class: "tag success" }, "Пройдено") : null
                    )
                ),
                el("div", { class: "row" },
                    el("button", { class: "btn small", onclick: () => addFavorite(q.id) }, "В избранное"),
                    isDone
                        ? el("button", { class: "btn small", disabled: "disabled" }, "Пройдено")
                        : el("button", { class: "btn primary small", onclick: () => startQuiz(q.id) }, "Пройти")
                )
            )
        ));
    }
    view.appendChild(box);
};

async function addFavorite(quizId) {
    if (!requireUser()) return;
    try {
        await api(`/api/users/${ME.user.id}/favorites/${quizId}`, { method: "POST" });
        toast("Добавлено в избранное");
    } catch (e) { toast(e.message); }
}

async function startQuiz(quizId) {
    if (!requireUser()) return;
    try {
        const r = await api(`/api/users/${ME.user.id}/quizzes/${quizId}/attempts`, { method: "POST" });
        location.hash = `#/attempt/${r.id}`;
    } catch (e) { toast(e.message); }
}

// --- Избранное ---
pages.favorites = async () => {
    setActive("favorites");
    if (!requireUser()) return;
    const data = await api(`/api/users/${ME.user.id}/favorites?size=50`);
    const list = data.content || [];
    view.appendChild(el("h1", {}, "Избранное"));
    if (!list.length) { view.appendChild(el("div", { class: "empty" }, "Избранных викторин нет")); return; }
    const box = el("div", { class: "list" });
    for (const f of list) {
        box.appendChild(el("div", { class: "card" },
            el("div", { class: "row between" },
                el("div", {}, el("h3", {}, f.quiz.title), el("div", { class: "muted" }, f.quiz.category?.name || "")),
                el("div", { class: "row" },
                    el("button", { class: "btn primary small", onclick: () => startQuiz(f.quiz.id) }, "Пройти"),
                    el("button", { class: "btn danger small", onclick: () => removeFavorite(f.quiz.id) }, "Удалить")
                )
            )
        ));
    }
    view.appendChild(box);
};

async function removeFavorite(quizId) {
    try {
        await api(`/api/users/${ME.user.id}/favorites/${quizId}`, { method: "DELETE" });
        toast("Удалено");
        render();
    } catch (e) { toast(e.message); }
}

// --- История ---
pages.history = async () => {
    setActive("history");
    if (!requireUser()) return;
    const data = await api(`/api/users/${ME.user.id}/history?size=50`);
    const list = data.content || [];
    view.appendChild(el("h1", {}, "История"));
    if (!list.length) { view.appendChild(el("div", { class: "empty" }, "История пуста")); return; }
    view.appendChild(el("div", { class: "card" },
        el("table", {},
            el("thead", {}, el("tr", {},
                el("th", {}, "Викторина"), el("th", {}, "Статус"), el("th", {}, "Балл"),
                el("th", {}, "Процент"), el("th", {}, "Завершено"), el("th", {}, "")
            )),
            el("tbody", {}, ...list.map((r) =>
                el("tr", {},
                    el("td", {}, r.quiz.title),
                    el("td", {}, el("span", { class: "tag" }, r.status)),
                    el("td", {}, r.score != null ? `${r.score}/${r.maxScore}` : "—"),
                    el("td", {}, r.percentage != null ? `${r.percentage}%` : "—"),
                    el("td", { class: "muted" }, fmtDate(r.finishedAt)),
                    el("td", {},
                        r.status === "IN_PROGRESS"
                            ? el("button", { class: "btn small", onclick: () => { location.hash = `#/attempt/${r.id}`; } }, "Продолжить")
                            : el("button", { class: "btn small", onclick: () => { location.hash = `#/review/${r.id}`; } }, "Обзор")
                    )
                )
            ))
        )
    ));
};

// --- Статистика ---
pages.stats = async () => {
    setActive("stats");
    if (!requireUser()) return;
    const s = await api(`/api/users/${ME.user.id}/statistics`);
    view.appendChild(el("h1", {}, "Статистика"));
    view.appendChild(el("div", { class: "split" },
        el("div", { class: "card" }, el("h3", {}, "Всего попыток"), el("h2", {}, String(s.totalAttempts))),
        el("div", { class: "card" }, el("h3", {}, "Завершено"), el("h2", {}, String(s.completedAttempts))),
        el("div", { class: "card" }, el("h3", {}, "В процессе"), el("h2", {}, String(s.inProgressAttempts))),
        el("div", { class: "card" }, el("h3", {}, "Средний процент"), el("h2", {}, `${s.averagePercentage}%`)),
        el("div", { class: "card" }, el("h3", {}, "Лучший балл"), el("h2", {}, String(s.bestScore)))
    ));
};

// --- Лидеры ---
pages.leaderboard = async () => {
    setActive("leaderboard");
    const data = await api("/api/leaderboard?limit=20");
    view.appendChild(el("h1", {}, "Таблица лидеров"));
    view.appendChild(el("p", {}, "Топ-20 лучших результатов по всем викторинам."));

    if (!data.length) {
        view.appendChild(el("div", { class: "empty" }, "Пока нет результатов"));
        return;
    }

    const podium = el("div", { class: "podium" });
    const list = el("div", { class: "list" });

    data.forEach((r, i) => {
        const rank = i + 1;

        if (rank <= 3) {
            podium.appendChild(el("div", { class: `card podium-${rank}` },
                el("div", { class: "rank-badge" }, `#${rank}`),
                el("h3", {}, r.user?.username || "Аноним"),
                el("div", { class: "muted", style: "margin-bottom:8px" }, r.quiz?.title || "—"),
                el("div", { class: "score" }, `${r.percentage}%`),
                el("div", { class: "muted" }, `${r.score} из ${r.maxScore} баллов`)
            ));
        } else {
            list.appendChild(el("div", { class: "card row between", style: "padding:12px 16px" },
                el("div", { class: "row" },
                    el("span", { class: "rank-num" }, `#${rank}`),
                    el("div", {},
                        el("div", { style: "font-weight:500" }, r.user?.username || "Аноним"),
                        el("div", { class: "muted", style: "font-size:12px" }, r.quiz?.title || "—")
                    )
                ),
                el("div", { style: "text-align:right" },
                    el("div", { style: "font-weight:600" }, `${r.percentage}%`),
                    el("div", { class: "muted", style: "font-size:12px" }, `${r.score}/${r.maxScore}`)
                )
            ));
        }
    });

    if (podium.children.length > 0) view.appendChild(podium);
    if (list.children.length > 0) view.appendChild(list);
};

// --- Прохождение ---
pages.attempt = async (resultId) => {
    setActive("");
    if (!requireUser()) return;
    const uid = ME.user.id;
    const attempt = await api(`/api/users/${uid}/attempts/${resultId}`);
    const [safeQuestions, answers] = await Promise.all([
        api(`/api/users/${uid}/attempts/${resultId}/questions-safe`),
        api(`/api/users/${uid}/attempts/${resultId}/answers`),
    ]);
    const answeredMap = new Map();
    for (const a of answers) answeredMap.set(a.question.id, new Set((a.selectedOptions || []).map((o) => o.id)));

    view.appendChild(el("h1", {}, attempt.quiz.title));
    view.appendChild(el("p", {}, `Статус: ${attempt.status}${attempt.deadlineAt ? " · дедлайн " + fmtDate(attempt.deadlineAt) : ""}`));

    const container = el("div", { class: "list" });
    for (const q of safeQuestions) {
        const selected = answeredMap.get(q.id) || new Set();
        const card = el("div", { class: "card" },
            el("div", { class: "row between" },
                el("h3", {}, `Вопрос ${q.orderIndex}. ${q.text}`),
                el("span", { class: "tag" }, q.type === "SINGLE_CHOICE" ? "Один ответ" : "Несколько")
            ),
            el("div", { class: "muted" }, `Баллов: ${q.points}`)
        );
        const optionsBox = el("div", { class: "options" });
        for (const o of q.options) {
            const isSel = selected.has(o.id);
            const opt = el("label", { class: "option" + (isSel ? " selected" : "") },
                el("input", {
                    type: q.type === "SINGLE_CHOICE" ? "radio" : "checkbox",
                    name: `q-${q.id}`, value: String(o.id), checked: isSel,
                    onchange: (e) => opt.classList.toggle("selected", e.target.checked),
                }),
                el("span", {}, o.text)
            );
            optionsBox.appendChild(opt);
        }
        card.appendChild(optionsBox);
        card.appendChild(el("div", { class: "row", style: "margin-top:16px" },
            el("button", { class: "btn primary small", onclick: () => saveAnswer(resultId, q, optionsBox) }, "Сохранить ответ"),
            el("button", { class: "btn small", onclick: () => skipAnswer(resultId, q.id) }, "Пропустить")
        ));
        container.appendChild(card);
    }
    view.appendChild(container);
    view.appendChild(el("div", { class: "row", style: "margin-top:32px" },
        el("button", { class: "btn primary", onclick: () => finishAttempt(resultId) }, "Завершить"),
        el("button", { class: "btn", onclick: () => { location.hash = "#/history"; } }, "К истории")
    ));
};

async function saveAnswer(resultId, q, box) {
    const checked = Array.from(box.querySelectorAll("input:checked")).map((i) => Number(i.value));
    if (!checked.length) { toast("Выберите хотя бы один вариант"); return; }
    try {
        await api(`/api/users/${ME.user.id}/attempts/${resultId}/questions/${q.id}/answer`, {
            method: "PUT", body: JSON.stringify(checked),
        });
        toast("Ответ сохранён");
    } catch (e) { toast(e.message); }
}

async function skipAnswer(resultId, questionId) {
    try {
        await api(`/api/users/${ME.user.id}/attempts/${resultId}/questions/${questionId}/answer`, { method: "DELETE" });
        toast("Вопрос пропущен");
    } catch (e) { toast(e.message); }
}

async function finishAttempt(resultId) {
    try {
        const r = await api(`/api/users/${ME.user.id}/attempts/${resultId}/finish`, { method: "PATCH" });
        toast(`Завершено: ${r.percentage}% (${r.score}/${r.maxScore})`);
        location.hash = `#/review/${resultId}`;
    } catch (e) { toast(e.message); }
}

// --- Обзор ---
pages.review = async (resultId) => {
    setActive("history");
    if (!requireUser()) return;
    const r = await api(`/api/users/${ME.user.id}/attempts/${resultId}/review`);
    view.appendChild(el("h1", {}, r.quiz.title));
    view.appendChild(el("p", {}, `Статус: ${r.status} · Процент: ${r.percentage}% · Балл: ${r.score}/${r.maxScore}`));
    view.appendChild(el("p", { class: "muted" }, `Начало: ${fmtDate(r.startedAt)} · Завершение: ${fmtDate(r.finishedAt)}`));
    const list = el("div", { class: "list" });
    for (const a of (r.answers || [])) {
        const card = el("div", { class: "card" }, el("h3", {}, a.question.text));
        const opts = el("div", { class: "options" });
        for (const o of (a.question.options || [])) {
            const chosen = (a.selectedOptions || []).some((s) => s.id === o.id);
            opts.appendChild(el("div", { class: "option" + (chosen ? " selected" : "") },
                el("span", {}, o.text + (o.correct ? " — верный" : "")),
                chosen ? el("span", { class: "tag", style: "margin-left:auto" }, "Выбрано") : null
            ));
        }
        card.appendChild(opts);
        list.appendChild(card);
    }
    view.appendChild(list);
    view.appendChild(el("div", { class: "row", style: "margin-top:24px" },
        el("button", { class: "btn", onclick: () => { location.hash = "#/history"; } }, "К истории")
    ));
};

// --- Админ ---
pages.admin = async () => {
    setActive("admin");
    if (!ME || !ME.isAdmin) {
        view.appendChild(el("h1", {}, "Админ-панель"));
        view.appendChild(el("div", { class: "empty" }, "Доступ только для администратора."));
        return;
    }
    const [stats, popular, results, categories] = await Promise.all([
        api("/api/admin/stats"),
        api("/api/admin/quizzes/popular?limit=5"),
        api("/api/admin/results?size=10"),
        api("/api/categories"),
    ]);

    view.appendChild(el("h1", {}, "Админ-панель"));

    // --- Статистика ---
    view.appendChild(el("div", { class: "split" },
        el("div", { class: "card" }, el("h3", {}, "Завершённых попыток"), el("h2", {}, String(stats.completedAttempts))),
        el("div", { class: "card" }, el("h3", {}, "Средний процент"), el("h2", {}, `${stats.averagePercentage}%`))
    ));

    // --- Управление викторинами ---
    view.appendChild(el("h2", {}, "Управление викторинами"));

    if (!categories.length) {
        view.appendChild(el("div", { class: "empty" }, "Сначала создайте хотя бы одну категорию ниже."));
    } else {
        const qCard = el("div", { class: "card" });
        const qTitle = el("input", { type: "text", placeholder: "Название викторины", style: "width:100%;margin-bottom:12px" });
        const qDesc = el("input", { type: "text", placeholder: "Описание", style: "width:100%;margin-bottom:12px" });
        const qCat = el("select", { style: "width:100%;margin-bottom:12px" });
        for (const c of categories) qCat.appendChild(el("option", { value: String(c.id) }, c.name));
        const qDiff = el("select", { style: "width:100%;margin-bottom:12px" });
        for (const d of ["EASY","MEDIUM","HARD"]) qDiff.appendChild(el("option", { value: d }, d));
        const qTime = el("input", { type: "number", placeholder: "Лимит времени (сек, пусто = без лимита)", style: "width:100%;margin-bottom:12px" });

        qCard.appendChild(qTitle); qCard.appendChild(qDesc); qCard.appendChild(qCat); qCard.appendChild(qDiff); qCard.appendChild(qTime);
        qCard.appendChild(el("button", { class: "btn primary", onclick: async () => {
                try {
                    const body = { title: qTitle.value, description: qDesc.value, difficulty: qDiff.value };
                    if (qTime.value) body.timeLimitSeconds = Number(qTime.value);
                    const newQuiz = await api(`/api/quizzes?categoryId=${qCat.value}`, { method: "POST", body: JSON.stringify(body) });
                    toast("Викторина создана. Теперь добавьте вопросы.");
                    openEditQuiz(newQuiz.id, newQuiz.title);
                    render();
                } catch (e) { toast(e.message); }
            }}, "Создать викторину и добавить вопросы"));
        view.appendChild(qCard);
    }

    const all = await api("/api/quizzes/all?size=100");
    const quizList = all.content || [];
    if (quizList.length) {
        view.appendChild(el("div", { class: "card", style: "margin-top:16px" },
            el("table", {},
                el("thead", {}, el("tr", {},
                    el("th", {}, "ID"), el("th", {}, "Название"), el("th", {}, "Статус"),
                    el("th", {}, "Категория"), el("th", {}, "Сложность"), el("th", {}, "")
                )),
                el("tbody", {}, ...quizList.map((q) =>
                    el("tr", {},
                        el("td", {}, String(q.id)),
                        el("td", {}, q.title),
                        el("td", {}, el("span", { class: "tag" }, q.status)),
                        el("td", {}, q.category?.name || "—"),
                        el("td", {}, q.difficulty),
                        el("td", {}, el("div", { class: "row" },
                            el("button", { class: "btn primary small", onclick: () => openEditQuiz(q.id, q.title) }, "Вопросы"),
                            q.status === "DRAFT"
                                ? el("button", { class: "btn small", onclick: () => publishQuiz(q.id) }, "Опубликовать")
                                : el("button", { class: "btn small", onclick: () => unpublishQuiz(q.id) }, "Снять")
                        ))
                    )
                ))
            )
        ));
    }

    // --- Категории ---
    view.appendChild(el("h2", {}, "Категории"));
    const catCard = el("div", { class: "card" });
    const catName = el("input", { type: "text", placeholder: "Название категории", style: "width:100%;margin-bottom:12px" });
    const catDesc = el("input", { type: "text", placeholder: "Описание", style: "width:100%;margin-bottom:12px" });
    catCard.appendChild(catName); catCard.appendChild(catDesc);
    catCard.appendChild(el("button", { class: "btn primary small", onclick: async () => {
            try {
                await api("/api/categories", { method: "POST", body: JSON.stringify({ name: catName.value, description: catDesc.value }) });
                toast("Категория создана"); render();
            } catch (e) { toast(e.message); }
        }}, "Создать категорию"));
    view.appendChild(catCard);

    if (categories.length) {
        view.appendChild(el("div", { class: "card" },
            el("table", {},
                el("thead", {}, el("tr", {}, el("th", {}, "ID"), el("th", {}, "Название"))),
                el("tbody", {}, ...categories.map((c) =>
                    el("tr", {}, el("td", {}, String(c.id)), el("td", {}, c.name))
                ))
            )
        ));
    }
};

async function publishQuiz(id) {
    try { await api(`/api/quizzes/${id}/publish`, { method: "PATCH" }); toast("Викторина опубликована"); render(); }
    catch (e) { toast(e.message); }
}
async function unpublishQuiz(id) {
    try { await api(`/api/quizzes/${id}/unpublish`, { method: "PATCH" }); toast("Снято с публикации"); render(); }
    catch (e) { toast(e.message); }
}

// --- Модалка редактирования викторины (Вопросы и Варианты) ---
async function openEditQuiz(quizId, title) {
    const modal = document.getElementById("edit-quiz-modal");
    const titleEl = document.getElementById("edit-quiz-title");
    const body = document.getElementById("edit-quiz-body");

    titleEl.textContent = `Вопросы: ${title}`;
    body.innerHTML = "Загрузка...";
    modal.classList.add("active");

    const quiz = await api(`/api/quizzes/${quizId}`);
    const questions = await api(`/api/quizzes/${quizId}/questions`);

    body.innerHTML = "";

    const addQCard = el("div", { class: "card", style: "background: #fafafa;" });
    addQCard.appendChild(el("h3", {}, "Добавить новый вопрос"));

    const qText = el("input", { type: "text", placeholder: "Текст вопроса", style: "width:100%;margin-bottom:12px" });
    const qType = el("select", { style: "width:100%;margin-bottom:12px" });
    qType.appendChild(el("option", { value: "SINGLE_CHOICE" }, "Один ответ"));
    qType.appendChild(el("option", { value: "MULTIPLE_CHOICE" }, "Несколько ответов"));
    const qPoints = el("input", { type: "number", placeholder: "Баллы", value: "1", style: "width:100%;margin-bottom:12px" });

    addQCard.appendChild(qText); addQCard.appendChild(qType); addQCard.appendChild(qPoints);
    addQCard.appendChild(el("button", { class: "btn primary small", onclick: async () => {
            try {
                const bodyData = { text: qText.value, type: qType.value, points: Number(qPoints.value) };
                await api(`/api/quizzes/${quizId}/questions`, { method: "POST", body: JSON.stringify(bodyData) });
                toast("Вопрос добавлен");
                openEditQuiz(quizId, title);
            } catch (e) { toast(e.message); }
        }}, "Добавить вопрос"));
    body.appendChild(addQCard);

    if (questions.length > 0) {
        const list = el("div", { class: "list", style: "margin-top: 24px;" });
        for (const q of questions) {
            const qCard = el("div", { class: "card" },
                el("div", { class: "row between" },
                    el("h3", {}, `№${q.orderIndex}. ${q.text}`),
                    el("span", { class: "tag" }, `${q.points} б.`)
                )
            );

            const optsBox = el("div", { class: "options" });
            for (const o of (q.options || [])) {
                optsBox.appendChild(el("div", { class: "option" + (o.correct ? " selected" : "") },
                    el("span", {}, o.text),
                    o.correct ? el("span", { class: "tag success", style: "margin-left:auto" }, "верный") : null
                ));
            }
            qCard.appendChild(optsBox);

            const oText = el("input", { type: "text", placeholder: "Текст варианта", style: "margin-top:16px" });
            const oCorrect = el("input", { type: "checkbox" });

            qCard.appendChild(el("div", { class: "row", style: "margin-top:12px" },
                oText,
                el("label", { class: "row" }, oCorrect, el("span", {}, "верный")),
                el("button", { class: "btn primary small", onclick: async () => {
                        try {
                            const bodyData = { text: oText.value, correct: oCorrect.checked };
                            await api(`/api/questions/${q.id}/options`, { method: "POST", body: JSON.stringify(bodyData) });
                            toast("Вариант добавлен");
                            openEditQuiz(quizId, title);
                        } catch (e) { toast(e.message); }
                    }}, "Добавить вариант")
            ));

            qCard.appendChild(el("div", { class: "row", style: "margin-top:12px" },
                el("button", { class: "btn danger small", onclick: async () => {
                        if (!confirm("Удалить вопрос?")) return;
                        try {
                            await api(`/api/questions/${q.id}`, { method: "DELETE" });
                            toast("Вопрос удален");
                            openEditQuiz(quizId, title);
                        } catch (e) { toast(e.message); }
                    }}, "Удалить вопрос")
            ));

            list.appendChild(qCard);
        }
        body.appendChild(list);
    }
}

function closeEditQuiz() {
    document.getElementById("edit-quiz-modal").classList.remove("active");
    render();
}