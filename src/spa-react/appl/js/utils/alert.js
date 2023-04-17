
    export function messageAlert(element, alertMessage, type) {
        const message = `<div style="width: 400px"
                        class="alert alert-${type} alert-dismissible fade show position-absolute top-0 start-50"
                        role="alert">
                        <button type="button" class="close" data-bs-dismiss="alert" aria-label="Close">
                        <span aria-hidden="true">&times</span></button>
                        ${alertMessage}
                        </div>`;
        element.append(message);
        if(typeof window.testit === "undefined" || !window.testit) {
            setTimeout(function () {
                    document.querySelector(".alert button").click();
            }, 3000);
        }
}
