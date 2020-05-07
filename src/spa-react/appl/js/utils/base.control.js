
import App from "../app";
import Helpers from "./helpers";
import { login } from "../login";

export default {
    defaults: {
    },
    init () {
    },
    view (options) {
        const loading = Helpers.getValueOrDefault(options.loading, false);

        // Lets not clutter up the test reporting.
        if ((typeof window.testit !== "undefined" && !window.testit) || window.__karma__ === undefined) {
            if (loading) {
                // In lieu of spinner for demo
                console.warn("Loading");
            }
        }

        const render = Helpers.renderer(this, options);

        if (options.template) {
            switch (options.template.split(".")[0]) {
                case "tools":
                    App.renderTools(options, render);
                    break;
                default:
            }
        } else {
            App.loadView(options, frag => {
                App.html = render(frag);
            });
        }
    },
    modal (options) {
        let template;

        App.loadView({
            url: "templates/stache/modal.stache"
        }, modalFrag => {
            template = Stache.compile(modalFrag);

            App.loadView(options, frag => {
                options["foot"] = Stache.compile(options.foot)(options);
                options["body"] = Stache.compile(frag)(options);
                const el = $(document.body).append(template(options)).find("> .modal").last();
                const css = {};
                
                if (options.width) {
                    css["width"] = typeof css.width === "number"
                        ? `${options.width}%` : options.width;
                    const width = css.width.substring(0, css.width.length - 1);
                    css["margin-left"] = `${(100 - width) / 2}%`;
                }

                const loginButton = $(".modal .submit-login");
                loginButton.on("click", login(loginButton, true));

                $("div .modal-footer .contact").on("click", this["div .modal-footer .contact click"]);
                
                $(el).on("show.bs.modal", () => {
                    if (options.fnLoad) {
                        options.fnLoad(el);
                    }
                }).on("hide.bs.modal", () => {
                    if (options.fnHide) {
                        options.fnHide(el);
                    }
                }).on("hidden.bs.modal", function () {
                    $(this).remove();
                }).modal("show").css(css).find("> .modal-dialog").addClass(options.widthClass);
            });
        });
    },
    hideModal () {
        // HIDE ANY OPEN MODAL WINDOWS
        $(".modal.in", this.element).modal("hide");
    }
};
