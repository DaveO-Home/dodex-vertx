import Contact from "../components/ContactC";
import { act } from "@testing-library/react";

export default function (timer, React) {
    /*
     * Test Form validation and submission.
     */
    describe("Contact Form Validation", () => {
        let contact;
        let submitObject;
        let nameObject;
        let emailObject;
        let commentObject;
        const mainContainer = "#main_container";

        it("Contact form - verify required fields", () => {
            act(() => {
                main.render(<Contact />);
            });

            contact = $(`${mainContainer} form`);
            nameObject = $("#inputName");
            emailObject = $("#inputEmail");
            commentObject = $("#inputComment");

            expect(nameObject[0].validity.valueMissing).toBe(true);
            expect(emailObject[0].validity.valueMissing).toBe(true);
            expect(commentObject[0].validity.valueMissing).toBe(true);
            expect(contact.find("input[type=checkbox]")[0].validity.valueMissing).toBe(false); // Not required
        });

        it("Contact form - validate populated fields, email mismatch.", done => {
            submitObject = contact.find("input[type=submit]");

            nameObject.val("me");
            emailObject.val("not-an-email-address");
            commentObject.val("Stuff");

            submitObject.click();

            expect(nameObject[0].validity.valueMissing).toBe(false);
            expect(nameObject[0].checkValidity()).toBe(true);
            expect(commentObject[0].validity.valueMissing).toBe(false);
            expect(commentObject[0].checkValidity()).toBe(true);
            expect(emailObject[0].validity.valueMissing).toBe(false);
            expect(emailObject[0].checkValidity()).toBe(false);
            expect(emailObject[0].validity.typeMismatch).toBe(true);

            expect(contact[0]).toBeInDOM();
            expect(contact[0]).toExist();

            done();
        });

        it("Contact form - validate email with valid email address.", done => {
            emailObject.val("ace@ventura.com");

            expect(emailObject[0].validity.typeMismatch).toBe(false);
            expect(emailObject[0].checkValidity()).toBe(true);

            done();
        });

        it("Contact form - validate form submission.", done => {
            let form = $(`${mainContainer} form`)[0];
            const numbers = timer(25, 50);

            expect(form).toBeInDOM();
            submitObject.click();

            const observable = numbers.subscribe(timer => {
                form = $(`${mainContainer} form`)[0];
                if ((typeof form === "undefined") || timer === 50) {
                    expect($(`${mainContainer} form`)[0]).not.toBeInDOM();
                    expect($(`${mainContainer} form`)[0]).not.toExist();
                    observable.unsubscribe();
                    if (timer !== 50) {
                        done();
                    }
                }
            });
        });
    });
}
