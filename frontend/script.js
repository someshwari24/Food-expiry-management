const BASE_URL = "https://food-expiry-management.onrender.com/api";

/* =========================================================
   Registration
========================================================= */

const registerForm = document.getElementById("registerForm");

if (registerForm) {
    registerForm.addEventListener(
        "submit",
        async function (event) {
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

            setMessage(
                message,
                "Registering...",
                ""
            );

            try {
                const response = await fetch(
                    `${BASE_URL}/register`,
                    {
                        method: "POST",
                        headers: {
                            "Content-Type":
                                "application/json"
                        },
                        body: JSON.stringify({
                            name,
                            email,
                            password
                        })
                    }
                );

                const data =
                    await readResponseJson(response);

                if (response.ok) {
                    setMessage(
                        message,
                        data.message ||
                            "Registration successful",
                        "success"
                    );

                    registerForm.reset();

                    setTimeout(() => {
                        window.location.href =
                            "login.html";
                    }, 1000);
                } else {
                    setMessage(
                        message,
                        data.message ||
                            "Registration failed",
                        "error"
                    );
                }
            } catch (error) {
                console.error(
                    "Registration error:",
                    error
                );

                setMessage(
                    message,
                    "Unable to connect to backend",
                    "error"
                );
            }
        }
    );
}

/* =========================================================
   Login
========================================================= */

const loginForm = document.getElementById("loginForm");

if (loginForm) {
    loginForm.addEventListener(
        "submit",
        async function (event) {
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

            setMessage(
                message,
                "Logging in...",
                ""
            );

            try {
                const response = await fetch(
                    `${BASE_URL}/login`,
                    {
                        method: "POST",
                        headers: {
                            "Content-Type":
                                "application/json"
                        },
                        body: JSON.stringify({
                            email,
                            password
                        })
                    }
                );

                const data =
                    await readResponseJson(response);

                if (response.ok) {
                    localStorage.setItem(
                        "userId",
                        data.userId
                    );

                    localStorage.setItem(
                        "userName",
                        data.name
                    );

                    localStorage.setItem(
                        "userEmail",
                        data.email
                    );

                    setMessage(
                        message,
                        data.message ||
                            "Login successful",
                        "success"
                    );

                    setTimeout(() => {
                        window.location.href =
                            "dashboard.html";
                    }, 600);
                } else {
                    setMessage(
                        message,
                        data.message ||
                            "Invalid email or password",
                        "error"
                    );
                }
            } catch (error) {
                console.error(
                    "Login error:",
                    error
                );

                setMessage(
                    message,
                    "Unable to connect to backend",
                    "error"
                );
            }
        }
    );
}

/* =========================================================
   Dashboard initialization
========================================================= */

const foodForm =
    document.getElementById("foodForm");

if (foodForm) {
    initializeDashboard();
}

function initializeDashboard() {
    const userId =
        localStorage.getItem("userId");

    const userName =
        localStorage.getItem("userName");

    if (!userId) {
        window.location.href = "login.html";
        return;
    }

    const welcomeUser =
        document.getElementById("welcomeUser");

    if (welcomeUser) {
        welcomeUser.textContent =
            `Welcome, ${userName || "User"}`;
    }

    addClickListener(
        "logoutButton",
        logout
    );

    addSubmitListener(
        "foodForm",
        saveFood
    );

    addClickListener(
        "searchButton",
        searchFoods
    );

    addClickListener(
        "showAllButton",
        loadFoods
    );

    addClickListener(
        "refreshButton",
        loadFoods
    );

    addClickListener(
        "showExpiringButton",
        loadExpiringFoods
    );

    addClickListener(
        "showExpiredButton",
        loadExpiredFoods
    );

    addClickListener(
        "cancelUpdateButton",
        resetFoodForm
    );

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
        .value
        .trim();

    const userId =
        localStorage.getItem("userId");

    if (!userId) {
        logout();
        return;
    }

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
            document
                .getElementById("quantity")
                .value
        ),

        purchaseDate: document
            .getElementById("purchaseDate")
            .value,

        expiryDate: document
            .getElementById("expiryDate")
            .value
    };

    const message =
        document.getElementById("foodMessage");

    if (!validateFoodData(foodData)) {
        setMessage(
            message,
            "Please enter valid food details",
            "error"
        );

        return;
    }

    setMessage(
        message,
        foodId
            ? "Updating food item..."
            : "Adding food item...",
        ""
    );

    try {
        let url;
        let method;

        if (foodId) {
            url = `${BASE_URL}/foods/update`;
            method = "PUT";

            /*
             * Java backend expects:
             * String id = input.getString("id");
             */
            foodData.id = foodId;
        } else {
            url = `${BASE_URL}/foods/add`;
            method = "POST";
        }

        const response = await fetch(
            url,
            {
                method,
                headers: {
                    "Content-Type":
                        "application/json"
                },
                body: JSON.stringify(foodData)
            }
        );

        const data =
            await readResponseJson(response);

        if (response.ok) {
            setMessage(
                message,
                data.message ||
                    (
                        foodId
                            ? "Food updated successfully"
                            : "Food added successfully"
                    ),
                "success"
            );

            resetFoodForm();

            await Promise.all([
                loadFoods(),
                loadDashboardStatistics()
            ]);
        } else {
            setMessage(
                message,
                data.message ||
                    "Operation failed",
                "error"
            );
        }
    } catch (error) {
        console.error(
            "Save food error:",
            error
        );

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
    const userId =
        localStorage.getItem("userId");

    if (!userId) {
        logout();
        return;
    }

    setTableTitle("All Food Items");
    showTableLoading();

    try {
        const response = await fetch(
            `${BASE_URL}/foods?userId=${
                encodeURIComponent(userId)
            }`
        );

        const data =
            await readResponseJson(response);

        if (!response.ok) {
            throw new Error(
                data.message ||
                    "Failed to retrieve foods"
            );
        }

        displayFoods(
            getFoodArray(data)
        );
    } catch (error) {
        console.error(
            "Load foods error:",
            error
        );

        displayTableError(
            error.message
        );
    }
}

/* =========================================================
   Search foods
========================================================= */

async function searchFoods() {
    const userId =
        localStorage.getItem("userId");

    if (!userId) {
        logout();
        return;
    }

    const itemName = document
        .getElementById("searchItemName")
        .value
        .trim();

    const category = document
        .getElementById("searchCategory")
        .value;

    const parameters =
        new URLSearchParams();

    parameters.append(
        "userId",
        userId
    );

    if (itemName) {
        parameters.append(
            "itemName",
            itemName
        );
    }

    if (category) {
        parameters.append(
            "category",
            category
        );
    }

    setTableTitle("Search Results");
    showTableLoading();

    try {
        const response = await fetch(
            `${BASE_URL}/foods/search?${parameters.toString()}`
        );

        const data =
            await readResponseJson(response);

        if (!response.ok) {
            throw new Error(
                data.message ||
                    "Search failed"
            );
        }

        displayFoods(
            getFoodArray(data)
        );
    } catch (error) {
        console.error(
            "Search error:",
            error
        );

        displayTableError(
            error.message
        );
    }
}

/* =========================================================
   Expiring-soon foods
========================================================= */

async function loadExpiringFoods() {
    const userId =
        localStorage.getItem("userId");

    if (!userId) {
        logout();
        return;
    }

    setTableTitle(
        "Items Expiring Within 7 Days"
    );

    showTableLoading();

    try {
        const response = await fetch(
            `${BASE_URL}/foods/expiring-soon?userId=${
                encodeURIComponent(userId)
            }&days=7`
        );

        const data =
            await readResponseJson(response);

        if (!response.ok) {
            throw new Error(
                data.message ||
                    "Failed to retrieve expiring items"
            );
        }

        displayFoods(
            getFoodArray(data)
        );
    } catch (error) {
        console.error(
            "Expiring foods error:",
            error
        );

        displayTableError(
            error.message
        );
    }
}

/* =========================================================
   Expired foods
========================================================= */

async function loadExpiredFoods() {
    const userId =
        localStorage.getItem("userId");

    if (!userId) {
        logout();
        return;
    }

    setTableTitle(
        "Expired Food Items"
    );

    showTableLoading();

    try {
        const response = await fetch(
            `${BASE_URL}/foods/expired?userId=${
                encodeURIComponent(userId)
            }`
        );

        const data =
            await readResponseJson(response);

        if (!response.ok) {
            throw new Error(
                data.message ||
                    "Failed to retrieve expired items"
            );
        }

        displayFoods(
            getFoodArray(data)
        );
    } catch (error) {
        console.error(
            "Expired foods error:",
            error
        );

        displayTableError(
            error.message
        );
    }
}

/* =========================================================
   Display food table
========================================================= */

function displayFoods(foods) {
    const tableBody =
        document.getElementById("foodTableBody");

    if (!tableBody) {
        return;
    }

    tableBody.innerHTML = "";

    if (!Array.isArray(foods)
            || foods.length === 0) {

        tableBody.innerHTML = `
            <tr>
                <td
                    colspan="7"
                    class="empty-row"
                >
                    No food items found
                </td>
            </tr>
        `;

        return;
    }

    foods.forEach(food => {
        const row =
            document.createElement("tr");

        const id =
            getFoodId(food);

        const statusData =
            calculateStatus(
                food.expiryDate
            );

        row.innerHTML = `
            <td>
                ${escapeHtml(food.itemName)}
            </td>

            <td>
                ${escapeHtml(food.category)}
            </td>

            <td>
                ${escapeHtml(food.quantity)}
            </td>

            <td>
                ${escapeHtml(food.purchaseDate)}
            </td>

            <td>
                ${escapeHtml(food.expiryDate)}
            </td>

            <td>
                <span
                    class="status ${statusData.className}"
                >
                    ${escapeHtml(statusData.text)}
                </span>
            </td>

            <td>
                <div class="action-buttons">
                    <button
                        type="button"
                        class="edit-button"
                    >
                        Edit
                    </button>

                    <button
                        type="button"
                        class="delete-button"
                    >
                        Delete
                    </button>
                </div>
            </td>
        `;

        const editButton =
            row.querySelector(".edit-button");

        const deleteButton =
            row.querySelector(".delete-button");

        editButton.addEventListener(
            "click",
            function () {
                editFood(food);
            }
        );

        deleteButton.addEventListener(
            "click",
            function () {
                deleteFood(id);
            }
        );

        tableBody.appendChild(row);
    });
}

/* =========================================================
   Edit food
========================================================= */

function editFood(food) {
    const foodId =
        getFoodId(food);

    if (!foodId) {
        alert(
            "Unable to identify this food item"
        );

        return;
    }

    document
        .getElementById("foodId")
        .value = foodId;

    document
        .getElementById("itemName")
        .value = food.itemName || "";

    document
        .getElementById("category")
        .value = food.category || "";

    document
        .getElementById("quantity")
        .value = food.quantity || "";

    document
        .getElementById("purchaseDate")
        .value = food.purchaseDate || "";

    document
        .getElementById("expiryDate")
        .value = food.expiryDate || "";

    document
        .getElementById("formTitle")
        .textContent =
            "Update Food Item";

    document
        .getElementById("saveButton")
        .textContent =
            "Update Food";

    document
        .getElementById("cancelUpdateButton")
        .classList
        .remove("hidden");

    setMessage(
        document.getElementById("foodMessage"),
        "",
        ""
    );

    window.scrollTo({
        top: 0,
        behavior: "smooth"
    });
}

/* =========================================================
   Delete food
========================================================= */

async function deleteFood(foodId) {
    if (!foodId) {
        alert(
            "Unable to identify this food item"
        );

        return;
    }

    const confirmed = window.confirm(
        "Are you sure you want to delete this food item?"
    );

    if (!confirmed) {
        return;
    }

    const userId =
        localStorage.getItem("userId");

    if (!userId) {
        logout();
        return;
    }

    try {
        const response = await fetch(
            `${BASE_URL}/foods/delete`,
            {
                method: "DELETE",
                headers: {
                    "Content-Type":
                        "application/json"
                },
                body: JSON.stringify({
                    /*
                     * Java backend expects:
                     * String id = input.getString("id");
                     */
                    id: foodId,
                    userId
                })
            }
        );

        const data =
            await readResponseJson(response);

        if (response.ok) {
            alert(
                data.message ||
                    "Food deleted successfully"
            );

            await Promise.all([
                loadFoods(),
                loadDashboardStatistics()
            ]);
        } else {
            alert(
                data.message ||
                    "Unable to delete food"
            );
        }
    } catch (error) {
        console.error(
            "Delete food error:",
            error
        );

        alert(
            "Unable to connect to backend"
        );
    }
}

/* =========================================================
   Dashboard statistics
========================================================= */

async function loadDashboardStatistics() {
    const userId =
        localStorage.getItem("userId");

    if (!userId) {
        return;
    }

    try {
        const encodedUserId =
            encodeURIComponent(userId);

        const [
            allResponse,
            expiringResponse,
            expiredResponse
        ] = await Promise.all([
            fetch(
                `${BASE_URL}/foods?userId=${encodedUserId}`
            ),

            fetch(
                `${BASE_URL}/foods/expiring-soon?userId=${encodedUserId}&days=7`
            ),

            fetch(
                `${BASE_URL}/foods/expired?userId=${encodedUserId}`
            )
        ]);

        const [
            allData,
            expiringData,
            expiredData
        ] = await Promise.all([
            readResponseJson(allResponse),
            readResponseJson(expiringResponse),
            readResponseJson(expiredResponse)
        ]);

        if (!allResponse.ok) {
            throw new Error(
                allData.message ||
                    "Failed to load total items"
            );
        }

        if (!expiringResponse.ok) {
            throw new Error(
                expiringData.message ||
                    "Failed to load expiring items"
            );
        }

        if (!expiredResponse.ok) {
            throw new Error(
                expiredData.message ||
                    "Failed to load expired items"
            );
        }

        const allFoods =
            getFoodArray(allData);

        const expiringFoods =
            getFoodArray(expiringData);

        const expiredFoods =
            getFoodArray(expiredData);

        setElementText(
            "totalItems",
            allFoods.length
        );

        setElementText(
            "expiringCount",
            expiringFoods.length
        );

        setElementText(
            "expiredCount",
            expiredFoods.length
        );
    } catch (error) {
        console.error(
            "Statistics error:",
            error
        );
    }
}

/* =========================================================
   Helper methods
========================================================= */

function getFoodArray(data) {
    if (Array.isArray(data)) {
        return data;
    }

    if (data
            && Array.isArray(data.foods)) {
        return data.foods;
    }

    if (data
            && Array.isArray(data.items)) {
        return data.items;
    }

    return [];
}

function getFoodId(food) {
    if (!food) {
        return "";
    }

    if (typeof food._id === "string") {
        return food._id;
    }

    if (food._id
            && typeof food._id.$oid === "string") {
        return food._id.$oid;
    }

    if (typeof food.foodId === "string") {
        return food.foodId;
    }

    if (typeof food.id === "string") {
        return food.id;
    }

    return "";
}

function calculateStatus(expiryDateValue) {
    if (!expiryDateValue) {
        return {
            text: "Unknown",
            className: "status-warning"
        };
    }

    const today = new Date();

    today.setHours(
        0,
        0,
        0,
        0
    );

    const expiryDate =
        new Date(
            `${expiryDateValue}T00:00:00`
        );

    if (Number.isNaN(
            expiryDate.getTime()
    )) {
        return {
            text: "Invalid Date",
            className: "status-warning"
        };
    }

    const difference =
        expiryDate.getTime()
        - today.getTime();

    const days =
        Math.ceil(
            difference /
            (
                1000
                * 60
                * 60
                * 24
            )
        );

    if (days < 0) {
        return {
            text: "Expired",
            className: "status-expired"
        };
    }

    if (days === 0) {
        return {
            text: "Expires Today",
            className: "status-warning"
        };
    }

    if (days <= 7) {
        return {
            text: `${days} day(s) left`,
            className: "status-warning"
        };
    }

    return {
        text: "Safe",
        className: "status-safe"
    };
}

function validateFoodData(foodData) {
    return Boolean(
        foodData.userId
        && foodData.itemName
        && foodData.category
        && Number.isFinite(
            foodData.quantity
        )
        && foodData.quantity > 0
        && foodData.purchaseDate
        && foodData.expiryDate
        && foodData.expiryDate
            >= foodData.purchaseDate
    );
}

function resetFoodForm() {
    const form =
        document.getElementById("foodForm");

    if (form) {
        form.reset();
    }

    const foodId =
        document.getElementById("foodId");

    if (foodId) {
        foodId.value = "";
    }

    setElementText(
        "formTitle",
        "Add Food Item"
    );

    setElementText(
        "saveButton",
        "Add Food"
    );

    const cancelButton =
        document.getElementById(
            "cancelUpdateButton"
        );

    if (cancelButton) {
        cancelButton.classList.add(
            "hidden"
        );
    }
}

function setTableTitle(title) {
    setElementText(
        "tableTitle",
        title
    );
}

function showTableLoading() {
    const tableBody =
        document.getElementById(
            "foodTableBody"
        );

    if (!tableBody) {
        return;
    }

    tableBody.innerHTML = `
        <tr>
            <td
                colspan="7"
                class="empty-row"
            >
                Loading...
            </td>
        </tr>
    `;
}

function displayTableError(message) {
    const tableBody =
        document.getElementById(
            "foodTableBody"
        );

    if (!tableBody) {
        return;
    }

    tableBody.innerHTML = `
        <tr>
            <td
                colspan="7"
                class="empty-row"
            >
                ${escapeHtml(
                    message ||
                    "Something went wrong"
                )}
            </td>
        </tr>
    `;
}

function setMessage(
    element,
    text,
    type
) {
    if (!element) {
        return;
    }

    element.textContent =
        text || "";

    element.className =
        type
            ? `message ${type}`
            : "message";
}

function setElementText(
    elementId,
    value
) {
    const element =
        document.getElementById(
            elementId
        );

    if (element) {
        element.textContent =
            String(value);
    }
}

function addClickListener(
    elementId,
    listener
) {
    const element =
        document.getElementById(
            elementId
        );

    if (element) {
        element.addEventListener(
            "click",
            listener
        );
    }
}

function addSubmitListener(
    elementId,
    listener
) {
    const element =
        document.getElementById(
            elementId
        );

    if (element) {
        element.addEventListener(
            "submit",
            listener
        );
    }
}

async function readResponseJson(
    response
) {
    const text =
        await response.text();

    if (!text) {
        return {};
    }

    try {
        return JSON.parse(text);
    } catch (error) {
        throw new Error(
            `Backend returned an invalid response (${response.status})`
        );
    }
}

function logout() {
    localStorage.removeItem("userId");
    localStorage.removeItem("userName");
    localStorage.removeItem("userEmail");

    window.location.href =
        "login.html";
}

function escapeHtml(value) {
    if (value === undefined
            || value === null) {
        return "";
    }

    return String(value)
        .replaceAll(
            "&",
            "&amp;"
        )
        .replaceAll(
            "<",
            "&lt;"
        )
        .replaceAll(
            ">",
            "&gt;"
        )
        .replaceAll(
            '"',
            "&quot;"
        )
        .replaceAll(
            "'",
            "&#039;"
        );
}