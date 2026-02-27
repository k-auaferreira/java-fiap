function redirectToPedidos() {
    const cpf = document.getElementById('cpfInput').value;
    if (cpf.length === 11) {
        window.location.href = `/pedidos/detalhe/${cpf}`;
    } else {
        alert('Por favor, insira um CPF válido com 11 dígitos.');
    }
}