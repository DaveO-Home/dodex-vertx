
import trimStart from "lodash/trimStart";

export default {
    // Bootstrap activation
    activate (selector) {
        let activated = false;
        // Ensure jquery
        const el = selector instanceof $ ? selector : $(selector);
        // Element is likely a list
        el.each(function () {
            const href = $("a", this).attr("href");
            const url = href ? trimStart(href, "#!") : "none";
            const hash = trimStart(window.location.hash, "#!");

            if (hash === url) {
                window.location.hash = "";

                $(this).addClass("active").siblings().removeClass("active");

                window.location.hash = `#!${hash}`;
                activated = true;

                return false;
            }
        });

        if (!activated) {
            el.removeClass("active");
        }
    }
};
