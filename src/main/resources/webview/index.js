// TODO: consider if needed
function handleSubmit(event) {
    try {
        window.api.postMessage(event);
    } catch (e) {
        // TODO: handle error
        alert(e);
    }
    event.preventDefault();
}

const form = document.getElementById('form');
form.addEventListener('submit', handleSubmit);
