/* Note; this is being handled in  the PdfC component */
import App from "../app";
import Base from "../utils/base.control";
Base.init();
export default App.controllers.Pdf || (App.controllers.Pdf = Object.assign({
    name: "pdf",
    finish (options) {
        $("#pdfDO").attr("src", options.pdfUrl);
    },
    test (options) {
        const pdfUrl = "views/prod/Test.pdf";

        this.view({
            local_content: "<iframe id=\"pdfDO\" name=\"pdfDO\" class=\"col-lg-12\" style=\"height: 750px\"></iframe>",
            pdfUrl: pdfUrl,
            controller: options.controller
        });
    }
}, Base));
