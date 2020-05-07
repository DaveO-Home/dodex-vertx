
export function messageAlert(element, alertMessage, type) {
    const message = `<div style="width: 400px" class="alert alert-${type} alert-dismissible fade show" role="alert">
                    <button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times</span></button>
                    ${alertMessage}
                    </div>`;
    element.append(message);
}
