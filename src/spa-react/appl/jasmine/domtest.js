export default function (resource, mainElement) {
    if (window.testit !== undefined && window.testit) {
        switch (resource) {
            case "index":
                expect(mainElement.querySelector("h1").textContent).toBe("Welcome To");
                break;
            case "pdf":
                expect($(mainElement).find("#data[src$=\"Test.pdf\"]").length > 0).toBe(true);
                break;
            case "tools":
                expect($(mainElement).find(".dropdown-menu").find(".dropdown-item").length > 2).toBe(true);
                break;
            default:
        }
    }
}
