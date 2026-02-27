document.addEventListener('DOMContentLoaded', function() {
    var userMenuButton = document.getElementById('userMenuButton');
    if (userMenuButton) {
        userMenuButton.addEventListener('click', function() {
            var menu = document.getElementById('userMenu');
            if (menu) {
                menu.classList.toggle('hidden');
            }
        });
    }
});