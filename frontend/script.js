const BASE_URL = "http://localhost:8080/api";

/* =========================================================
   Registration
========================================================= */

const registerForm = document.getElementById("registerForm");

if (registerForm) {
    registerForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const name = document
            .getElementById("registerName")
            .value
            .trim();

        const email = document
            .getElementById("registerEmail")
            .value
            .trim();

        const password = document
            .getElementById("registerPassword")
            .value;

        const message =
            document.getElementById("registerMessage");

        setMessage(message, "Registering...", "");

        try {
            const response = await fetch(
                `${BASE_URL}/register`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        name,
                        email,
                        password
                    })
                }
            );

            const data = await response.json();

            if (response.ok) {
                setMessage(
                    message,
                    data.message || "Registration successful",
                    "success"
                );

                registerForm.reset();

                setTimeout(() => {
                    window.location.href = "login.html";
                }, 1000);
            } else {
                setMessage(
                    message,
                    data.message || "Registration failed",
                    "error"
                );
            }
        } catch (error) {
            console.error(error);

            setMessage(
                message,
                "Unable to connect to backend",
                "error"
            );
        }
    });
}

/* =========================================================
   Login
========================================================= */

const loginForm = document.getElementById("loginForm");

if (loginForm) {
    loginForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const email = document
            .getElementById("loginEmail")
            .value
            .trim();

        const password = document
            .getElementById("loginPassword")
            .value;

        const message =
            document.getElementById("loginMessage");

        setMessage(message, "Logging in...", "");

        try {
            const response = await fetch(
                `${BASE_URL}/login`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        email,
                        password
                    })
                }
            );

            const data = await response.json();

            if (response.ok) {
                localStorage.setItem("userId", data.userId);
                localStorage.setItem("userName", data.name);
                localStorage.setItem("userEmail", data.email);

                setMessage(
                    message,
                    data.message || "Login successful",
                    "success"
                );

                setTimeout(() => {
                    window.location.href = "dashboard.html";
                }, 600);
            } else {
                setMessage(
                    message,
                    data.message || "Invalid email or password",
                    "error"
                );
            }
        } catch (error) {
            console.error(error);

            setMessage(
                message,
                "Unable to connect to backend",
                "error"
            );
        }
    });
}

/* =========================================================
   Dashboard initialization
========================================================= */

const foodForm = document.getElementById("foodForm");

if (foodForm) {
    initializeDashboard();
}

function initializeDashboard() {
    const userId = localStorage.getItem("userId");
    const userName = localStorage.getItem("userName");

    if (!userId) {
        window.location.href = "login.html";
        return;
    }

    document.getElementById("welcomeUser").textContent =
        `Welcome, ${userName || "User"}`;

    document
        .getElementById("logoutButton")
        .addEventListener("click", logout);

    document
        .getElementById("foodForm")
        .addEventListener("submit", saveFood);

    document
        .getElementById("searchButton")
        .addEventListener("click", searchFoods);

    document
        .getElementById("showAllButton")
        .addEventListener("click", loadFoods);

    document
        .getElementById("refreshButton")
        .addEventListener("click", loadFoods);

    document
        .getElementById("showExpiringButton")
        .addEventListener("click", loadExpiringFoods);

    document
        .getElementById("showExpiredButton")
        .addEventListener("click", loadExpiredFoods);

    document
        .getElementById("cancelUpdateButton")
        .addEventListener("click", resetFoodForm);

    loadFoods();
    loadDashboardStatistics();
}

/* =========================================================
   Add or Update Food
========================================================= */

async function saveFood(event) {
    event.preventDefault();

    const foodId = document
        .getElementById("foodId")
        .value;

    const userId = localStorage.getItem("userId");

    const foodData = {
        userId,
        itemName: document
            .getElementById("itemName")
            .value
            .trim(),

        category: document
            .getElementById("category")
            .value,

        quantity: Number(
            document.getElementById("quantity").value
        ),

        purchaseDate: document
            .getElementById("purchaseDate")
            .value,

        expiryDate: document
            .getElementById("expiryDate")
            .value
    };

    const message = document.getElementById("foodMessage");

    try {
        let url;
        let method;

        if (foodId) {
            url = `${BASE_URL}/foods/update`;
            method = "PUT";

            foodData.foodId = foodId;
        } else {
            url = `${BASE_URL}/foods/add`;
            method = "POST";
        }

        const response = await fetch(url, {
            method,
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(foodData)
        });

        const data = await response.json();

        if (response.ok) {
            setMessage(
                message,
                data.message ||
                    (foodId
                        ? "Food updated successfully"
                        : "Food added successfully"),
                "success"
            );

            resetFoodForm();
            await loadFoods();
            await loadDashboardStatistics();
        } else {
            setMessage(
                message,
                data.message || "Operation failed",
                "error"
            );
        }
    } catch (error) {
        console.error(error);

        setMessage(
            message,
            "Unable to connect to backend",
            "error"
        );
    }
}

/* =========================================================
   Retrieve all foods
========================================================= */

async function loadFoods() {
    const userId = localStorage.getItem("userId");

    document.getElementById("tableTitle").textContent =
        "All Food Items";

    try {
        const response = await fetch(
            `${BASE_URL}/foods?userId=${encodeURIComponent(userId)}`
        );

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || "Failed to retrieve foods");
        }

        const foods = getFoodArray(data);

        displayFoods(foods);
    } catch (error) {
        console.error(error);
        displayTableError(error.message);
    }
}

/* =========================================================
   Search foods
========================================================= */

async function searchFoods() {
    const userId = localStorage.getItem("userId");

    const itemName = document
        .getElementById("searchItemName")
        .value
        .trim();

    const category = document
        .getElementById("searchCategory")
        .value;

    const parameters = new URLSearchParams();

    parameters.append("userId", userId);

    if (itemName) {
        parameters.append("itemName", itemName);
    }

    if (category) {
        parameters.append("category", category);
    }

    document.getElementById("tableTitle").textContent =
        "Search Results";

    try {
        const response = await fetch(
            `${BASE_URL}/foods/search?${parameters.toString()}`
        );

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || "Search failed");
        }

        displayFoods(getFoodArray(data));
    } catch (error) {
        console.error(error);
        displayTableError(error.message);
    }
}

/* =========================================================
   Expiring-soon foods
========================================================= */

async function loadExpiringFoods() {
    const userId = localStorage.getItem("userId");

    document.getElementById("tableTitle").textContent =
        "Items Expiring Within 7 Days";

    try {
        const response = await fetch(
            `${BASE_URL}/foods/expiring-soon?userId=${encodeURIComponent(userId)}&days=7`
        );

        const data = await response.json();

        if (!response.ok) {
            throw new Error(
                data.message || "Failed to retrieve expiring items"
            );
        }

        displayFoods(getFoodArray(data));
    } catch (error) {
        console.error(error);
        displayTableError(error.message);
    }
}

/* =========================================================
   Expired foods
========================================================= */

async function loadExpiredFoods() {
    const userId = localStorage.getItem("userId");

    document.getElementById("tableTitle").textContent =
        "Expired Food Items";

    try {
        const response = await fetch(
            `${BASE_URL}/foods/expired?userId=${encodeURIComponent(userId)}`
        );

        const data = await response.json();

        if (!response.ok) {
            throw new Error(
                data.message || "Failed to retrieve expired items"
            );
        }

        displayFoods(getFoodArray(data));
    } catch (error) {
        console.error(error);
        displayTableError(error.message);
    }
}

/* =========================================================
   Display food table
========================================================= */

function displayFoods(foods) {
    const tableBody =
        document.getElementById("foodTableBody");

    tableBody.innerHTML = "";

    if (!foods || foods.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="7" class="empty-row">
                    No food items found
                </td>
            </tr>
        `;

        return;
    }

    foods.forEach(food => {
        const row = document.createElement("tr");

        const id = food._id || food.foodId || food.id;

        const statusData = calculateStatus(food.expiryDate);

        row.innerHTML = `
            <td>${escapeHtml(food.itemName)}</td>

            <td>${escapeHtml(food.category)}</td>

            <td>${food.quantity}</td>

            <td>${escapeHtml(food.purchaseDate)}</td>

            <td>${escapeHtml(food.expiryDate)}</td>

            <td>
                <span class="status ${statusData.className}">
                    ${statusData.text}
                </span>
            </td>

            <td>
                <div class="action-buttons">
                    <button
                        type="button"
                        class="edit-button"
                        data-id="${id}"
                    >
                        Edit
                    </button>

                    <button
                        type="button"
                        class="delete-button"
                        data-id="${id}"
                    >
                        Delete
                    </button>
                </div>
            </td>
        `;

        row
            .querySelector(".edit-button")
            .addEventListener("click", function () {
                editFood(food);
            });

        row
            .querySelector(".delete-button")
            .addEventListener("click", function () {
                deleteFood(id);
            });

        tableBody.appendChild(row);
    });
}

/* =========================================================
   Edit food
========================================================= */

function editFood(food) {
    const foodId =
        food._id || food.foodId || food.id;

    document.getElementById("foodId").value =
        foodId;

    document.getElementById("itemName").value =
        food.itemName || "";

    document.getElementById("category").value =
        food.category || "";

    document.getElementById("quantity").value =
        food.quantity || "";

    document.getElementById("purchaseDate").value =
        food.purchaseDate || "";

    document.getElementById("expiryDate").value =
        food.expiryDate || "";

    document.getElementById("formTitle").textContent =
        "Update Food Item";

    document.getElementById("saveButton").textContent =
        "Update Food";

    document
        .getElementById("cancelUpdateButton")
        .classList
        .remove("hidden");

    window.scrollTo({
        top: 0,
        behavior: "smooth"
    });
}

/* =========================================================
   Delete food
========================================================= */

async function deleteFood(foodId) {
    const confirmed = confirm(
        "Are you sure you want to delete this food item?"
    );

    if (!confirmed) {
        return;
    }

    const userId = localStorage.getItem("userId");

    try {
        const response = await fetch(
            `${BASE_URL}/foods/delete`,
            {
                method: "DELETE",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    foodId,
                    userId
                })
            }
        );

        const data = await response.json();

        if (response.ok) {
            alert(data.message || "Food deleted successfully");

            await loadFoods();
            await loadDashboardStatistics();
        } else {
            alert(data.message || "Unable to delete food");
        }
    } catch (error) {
        console.error(error);
        alert("Unable to connect to backend");
    }
}

/* =========================================================
   Dashboard statistics
========================================================= */

async function loadDashboardStatistics() {
    const userId = localStorage.getItem("userId");

    try {
        const [allResponse, expiringResponse, expiredResponse] =
            await Promise.all([
                fetch(
                    `${BASE_URL}/foods?userId=${encodeURIComponent(userId)}`
                ),

                fetch(
                    `${BASE_URL}/foods/expiring-soon?userId=${encodeURIComponent(userId)}&days=7`
                ),

                fetch(
                    `${BASE_URL}/foods/expired?userId=${encodeURIComponent(userId)}`
                )
            ]);

        const allData = await allResponse.json();
        const expiringData = await expiringResponse.json();
        const expiredData = await expiredResponse.json();

        const allFoods = getFoodArray(allData);
        const expiringFoods = getFoodArray(expiringData);
        const expiredFoods = getFoodArray(expiredData);

        document.getElementById("totalItems").textContent =
            allFoods.length;

        document.getElementById("expiringCount").textContent =
            expiringFoods.length;

        document.getElementById("expiredCount").textContent =
            expiredFoods.length;
    } catch (error) {
        console.error("Statistics error:", error);
    }
}

/* =========================================================
   Helper methods
========================================================= */

function getFoodArray(data) {
    if (Array.isArray(data)) {
        return data;
    }

    if (Array.isArray(data.foods)) {
        return data.foods;
    }

    if (Array.isArray(data.items)) {
        return data.items;
    }

    return [];
}

function calculateStatus(expiryDateValue) {
    const today = new Date();

    today.setHours(0, 0, 0, 0);

    const expiryDate = new Date(`${expiryDateValue}T00:00:00`);

    const difference =
        expiryDate.getTime() - today.getTime();

    const days =
        Math.ceil(difference / (1000 * 60 * 60 * 24));

    if (days < 0) {
        return {
            text: "Expired",
            className: "status-expired"
        };
    }

    if (days <= 7) {
        return {
            text: days === 0
                ? "Expires Today"
                : `${days} day(s) left`,
            className: "status-warning"
        };
    }

    return {
        text: "Safe",
        className: "status-safe"
    };
}

function resetFoodForm() {
    document.getElementById("foodForm").reset();

    document.getElementById("foodId").value = "";

    document.getElementById("formTitle").textContent =
        "Add Food Item";

    document.getElementById("saveButton").textContent =
        "Add Food";

    document
        .getElementById("cancelUpdateButton")
        .classList
        .add("hidden");
}

function displayTableError(message) {
    document.getElementById("foodTableBody").innerHTML = `
        <tr>
            <td colspan="7" class="empty-row">
                ${escapeHtml(message)}
            </td>
        </tr>
    `;
}

function setMessage(element, text, type) {
    element.textContent = text;

    element.className = type
        ? `message ${type}`
        : "message";
}

function logout() {
    localStorage.removeItem("userId");
    localStorage.removeItem("userName");
    localStorage.removeItem("userEmail");

    window.location.href = "login.html";
}

function escapeHtml(value) {
    if (value === undefined || value === null) {
        return "";
    }

    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}