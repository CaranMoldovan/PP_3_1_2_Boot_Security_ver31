document.addEventListener('DOMContentLoaded', function () {

    const rolePanels = document.querySelectorAll('.rolePanel');
    const roleLinks = document.querySelectorAll('.roleLink');

    function hideAllRolePanels() {
        rolePanels.forEach(panel => panel.style.display = 'none');
    }

    function resetActiveRoleStyle() {
        const activeRole = document.querySelector('.bg-primary');
        if (activeRole) {
            activeRole.classList.remove('bg-primary', 'text-white');
        }
    }

    roleLinks.forEach(link => {
        link.addEventListener('click', event => {
            event.preventDefault();

            resetActiveRoleStyle();
            hideAllRolePanels();

            event.target.classList.add('bg-primary', 'text-white');
            const rolePanel =
                document.getElementById(
                    `${event.target
                        .getAttribute('data-role')
                        .toLowerCase()}Panel`);
            if (rolePanel) {
                rolePanel.style.display = 'block';
            }
        });
    });

    hideAllRolePanels();
    document.getElementById('adminPanel').style.display = 'block';
    // код выше реализует смену ролей в админ панели в левой части страницы
    function fillFormWithData(button,
                              userIdInputId,
                              firstNameInputId,
                              lastNameInputId,
                              ageInputId,
                              emailInputId) {
        let id = button.data('id');
        let firstname = button.data('firstname');
        let lastname = button.data('lastname');
        let age = button.data('age');
        let email = button.data('email');

        $(userIdInputId).val(id);
        $(firstNameInputId).val(firstname);
        $(lastNameInputId).val(lastname);
        $(ageInputId).val(age);
        $(emailInputId).val(email);
    }

    let originalUserRoles = null;
    // для отображения данных в модальном окне редактирования пользователя
    $('.edit-user-button').click(function () {
        fillFormWithData($(this),
            '#editUserId',
            '#editFirstName',
            '#editLastName',
            '#editAge',
            '#editEmail');
        // запоминаем ранее выбранные роли пользователя
        originalUserRoles = $(this).data('role');
    });

    // для отображения данных в модальном окне удаления пользователя
    $('.delete-user-button').click(function () {
        fillFormWithData($(this),
            '#deleteUserId',
            '#deleteUserFirstName',
            '#deleteUserLastName',
            '#deleteUserAge',
            '#deleteUserEmail');
        $('#deleteUserRole').val($(this).data('role'));
    });

    // обработка кнопки сохранения в модальном окне редактирования пользователя.
    document.querySelector('#editUserModal .save-changes').addEventListener('click', function (e) {
        e.preventDefault();

        let id = document.querySelector('#editUserId').value;
        let firstname = document.querySelector('#editFirstName').value;
        let lastname = document.querySelector('#editLastName').value;
        let age = document.querySelector('#editAge').value;
        let email = document.querySelector('#editEmail').value;
        let password = document.querySelector('#editPassword').value;

        let token = $("meta[name='_csrf']").attr("content");
        let header = $("meta[name='_csrf_header']").attr("content");

        let selectElement = document.querySelector('#editRole');
        let selectedOptions = Array.from(selectElement.selectedOptions);
        let roles = selectedOptions.map(option => {
            if (option.value === "User") {
                return "ROLE_USER";
            } else if (option.value === "Admin") {
                return "ROLE_ADMIN";
            }
        });

        let params = new URLSearchParams({
            id: id,
            firstname: firstname,
            lastname: lastname,
            age: age,
            email: email,
            password: password,
        });

        roles.forEach(role => params.append('role', role)); // Добавляем каждую роль

        fetch('/admin/update', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                [header]: token
            },
            body: params,
        })
            .then(response => {
                $('#editUserModal').modal('hide');
                location.reload();
            });
    });

    // обработка кнопки Delete в модальном окне удаления пользователя.
    document.querySelector('.delete-user-button-confirm').addEventListener('click', function (e) {
        e.preventDefault();

        let id = document.querySelector('#deleteUserId').value;

        let token = $("meta[name='_csrf']").attr("content");
        let header = $("meta[name='_csrf_header']").attr("content");

        fetch('/admin/delete', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                [header]: token
            },
            body: new URLSearchParams({
                id: id
            }),
        })
            .then(response => {
                $('#deleteUserModal').modal('hide');
                location.reload();
            })
    });

// обработка кнопки Add new user в дополнительной вкладке New user.
    document.querySelector('.add-user-form').addEventListener('submit', function (event) {
        event.preventDefault();

        let firstname = document.querySelector('#new_firstName').value;
        let lastname = document.querySelector('#new_lastName').value;
        let age = document.querySelector('#new_age').value;
        let email = document.querySelector('#new_email').value;
        let password = document.querySelector('#new_password').value;

        let token = $("meta[name='_csrf']").attr("content");
        let header = $("meta[name='_csrf_header']").attr("content");

        let selectElement = document.querySelector('#new_role');
        let selectedOptions = Array.from(selectElement.selectedOptions);
        let roles = selectedOptions.map(option => {
            if (option.value === "User") {
                return "ROLE_USER";
            } else if (option.value === "Admin") {
                return "ROLE_ADMIN";
            }
        });

        let params = new URLSearchParams({
            firstname: firstname,
            lastname: lastname,
            age: age,
            email: email,
            password: password,
        });

        roles.forEach(role => params.append('role', role)); // Добавляем каждую роль

        fetch('/admin/add', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                [header]: token
            },
            body: params,
        })
            .then(response => {
                location.reload();
            });
    });

});