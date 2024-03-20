const CURRENT_USER_URL = "/api/user/current";
const CSRF_TOKEN_NAME = "_csrf";
const CSRF_HEADER_NAME = "X-CSRF-TOKEN";
const LOGOUT_URL = "/logout";
const SUCCESS_LOGOUT_URL = "/login?logout";
const LOGOUT_BUTTON_CLASS = ".logout-button";

const userIdElement = document.getElementById('current-user-id');
const userFirstNameElement = document.getElementById('current-user-firstname');
const userLastNameElement = document.getElementById('current-user-lastname');
const userAgeElement = document.getElementById('current-user-age');
const userEmailElement = document.getElementById('current-user-email');
const userRolesElement = document.getElementById('current-user-roles');

const headerUserEmailElement = document.getElementById('user-email');
const headerUserRolesElement = document.getElementById('user-roles');

const formatRoles = roles =>
    roles.map(role => `<span>${role.name.replace('ROLE_', '')}</span>`).join(', ');

window.onload = async function () {
    try {
        const response = await fetch(CURRENT_USER_URL);
        const data = await response.json();

        userIdElement.textContent = data.id;
        userFirstNameElement.textContent = data.firstname;
        userLastNameElement.textContent = data.lastname;
        userAgeElement.textContent = data.age;
        userEmailElement.textContent = data.email;
        userRolesElement.innerHTML = formatRoles(data.roles);

        headerUserEmailElement.textContent = data.email;
        headerUserRolesElement.innerHTML = formatRoles(data.roles);
    } catch (error) {
        console.error("Error:", error);
    }
};

document.querySelector(LOGOUT_BUTTON_CLASS).addEventListener("click", async function (event) {
    event.preventDefault();

    const token = document.querySelector(`meta[name="${CSRF_TOKEN_NAME}"]`).getAttribute("content");
    const headers = new Headers({
        "Content-Type": "application/json",
        [CSRF_HEADER_NAME]: token
    });

    try {
        const response = await fetch(LOGOUT_URL, {
            method: "POST",
            headers: headers,
            body: "dummy=data"
        });

        if (!response.ok) {
            console.error(`Logout failed: ${response.statusText}`);
        }

        window.location.href = SUCCESS_LOGOUT_URL;
    } catch (error) {
        console.error("Error:", error);
    }
});