import ToolsSM from "../js/utils/tools.sm";

export default function (ToolsC, Helpers, ReactDOM, React, timer) {
    /*
     * Test that new data are loaded when the select value changes.
     */
    describe("Load new tools page", function () {
        let tools;
        let beforeValue;
        let afterValue;
        let spyToolsEvent;
        let selectorObject;
        let selectorItem;
        let defaultReduxValue;
        let newReduxValue;

        beforeAll(function (done) {
            $("#dropdown1").remove();
            ReactDOM.render(
                <ToolsC />,
                document.querySelector("#main_container")
            );
            // Wait for Web Page to be loaded
            Helpers.getResource(ReactDOM, "main_container", 0, 0)
                .catch(function (rejected) {
                    fail("The Tools Page did not load within limited time: " + rejected);
                }).then(function () {
                    tools = $("#tools");
                    beforeValue = tools.find("tbody").find("tr:nth-child(1)").find("td:nth-child(2)").text();
                    defaultReduxValue = $("#tools-state").text().split(" ", 1);

                    selectorObject = $("#dropdown0");
                    selectorObject = document.activeElement;
                    selectorObject.click();
                    selectorItem = $("#dropdown1 a")[1];
                    spyToolsEvent = spyOnEvent(selectorItem, "select");
                    selectorItem.click();
                    Helpers.fireEvent(selectorItem, "select");
                    // Note: if page does not refresh, increase the timer time.
                    // Using RxJs instead of Promise.
                    const numbers = timer(50, 50);
                    const observable = numbers.subscribe(timer => {
                        afterValue = tools.find("tbody").find("tr:nth-child(1)").find("td:nth-child(2)").text();
                        if (afterValue !== beforeValue || timer === 25) {
                            newReduxValue = $("#tools-state").text().split(" ", 1);
                            observable.unsubscribe();
                            done();
                        }
                    });
                });
        });

        it("setup and click events executed.", function () {
            // jasmine-jquery matchers
            expect("select").toHaveBeenTriggeredOn(selectorItem);
            expect(spyToolsEvent).toHaveBeenTriggered();

            expect(tools[0]).toBeInDOM();
            expect(".disabled").toBeDisabled();
            expect("#dropdown1 a").toHaveLength(3);
            // Required for Firefox
            selectorObject[0] = document.activeElement;
            expect(selectorObject).toBeFocused();
        });

        it("did Redux set default value.", function () {
            expect(defaultReduxValue[0]).toBe("Combined");
        });

        it("new page loaded on change.", function () {
            expect(beforeValue).not.toBe(afterValue);
        });

        it("did Redux set new value.", function () {
            expect(newReduxValue[0]).toBe("Category1");
        });

        it("verify state management", function () {
            const items = ToolsSM.getStore().getState().tools.items;
            expect(items.length).toBe(2);
            expect(items[0].displayed).toBe(false);
            expect(items[1].displayed).toBe(true);
            expect(items[1].message).toBe("Category1");
        });
    });
}
